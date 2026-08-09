package com.melloo.mellooessentials.client.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Thin client for sky.melloo.me's EXISTING mod-auth + presence routes - reused as-is (no new
 * server-side code at all): the ephemeral-keypair handshake ({@code /mod/auth/challenge}/
 * {@code /mod/auth/verify}, see {@link ModAuthManager}) only proves a live Mojang session for a
 * real Minecraft account, nothing account/Discord-specific, so any mod can complete it. Presence
 * report/query ({@code /presence}, {@code /presence/query}) already return each nearby player's
 * reported cosmetics AND their server-resolved sky.melloo.me team role, which is exactly what's
 * needed here for cosmetics sync and staff highlighting - no separate backend needed.
 */
public final class ApiClient {
	private static final String BASE_URL = "https://sky.melloo.me/api";
	private static final HttpClient HTTP = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.build();

	private ApiClient() {
	}

	private static CompletableFuture<JsonObject> getJson(String path, ModAuthManager.ModIdentity identity) {
		HttpRequest.Builder builder = HttpRequest.newBuilder()
				.uri(URI.create(BASE_URL + path))
				.timeout(Duration.ofSeconds(8))
				.header("X-MellooEssentials-Client", "mod")
				.GET();
		if (identity != null) {
			attachSignature(builder, identity, "GET", requestPath(path), new byte[0]);
		}
		return sendWithRetry(builder.build())
				.thenApply(response -> {
					if (response.statusCode() != 200) {
						throw new RuntimeException(extractErrorMessage(response.body(), response.statusCode()));
					}
					JsonElement parsed = JsonParser.parseString(response.body());
					if (!parsed.isJsonObject()) {
						throw new RuntimeException("No data found");
					}
					return parsed.getAsJsonObject();
				});
	}

	private static CompletableFuture<JsonObject> postJson(String path, JsonObject body, ModAuthManager.ModIdentity identity) {
		byte[] bodyBytes = body.toString().getBytes(StandardCharsets.UTF_8);
		HttpRequest.Builder builder = HttpRequest.newBuilder()
				.uri(URI.create(BASE_URL + path))
				.timeout(Duration.ofSeconds(8))
				.header("Content-Type", "application/json")
				.header("X-MellooEssentials-Client", "mod")
				.POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes));
		if (identity != null) {
			attachSignature(builder, identity, "POST", requestPath(path), bodyBytes);
		}
		return sendWithRetry(builder.build())
				.thenApply(response -> {
					if (response.statusCode() != 200) {
						throw new RuntimeException(extractErrorMessage(response.body(), response.statusCode()));
					}
					JsonElement parsed = JsonParser.parseString(response.body());
					if (!parsed.isJsonObject()) {
						throw new RuntimeException("No data found");
					}
					return parsed.getAsJsonObject();
				});
	}

	/** A single one-second retry on a plain timeout - see sky.melloo.ch's SkyMellooApiClient, same reasoning. */
	private static CompletableFuture<HttpResponse<String>> sendWithRetry(HttpRequest request) {
		return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.handle((response, error) -> {
					if (error == null || !isTimeout(error)) {
						if (error != null) {
							CompletableFuture<HttpResponse<String>> failed = new CompletableFuture<>();
							failed.completeExceptionally(error);
							return failed;
						}
						return CompletableFuture.completedFuture(response);
					}
					return CompletableFuture
							.supplyAsync(() -> null, CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS))
							.thenCompose(ignored -> HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString()));
				})
				.thenCompose(future -> future);
	}

	private static boolean isTimeout(Throwable error) {
		Throwable cause = error;
		while (cause != null) {
			if (cause instanceof HttpTimeoutException) {
				return true;
			}
			cause = cause.getCause();
		}
		return false;
	}

	private static String extractErrorMessage(String body, int statusCode) {
		try {
			JsonElement parsed = JsonParser.parseString(body);
			if (parsed.isJsonObject() && parsed.getAsJsonObject().has("error")) {
				return parsed.getAsJsonObject().get("error").getAsString();
			}
		} catch (Exception ignored) {
			// fall back to the generic status message below
		}
		return "HTTP " + statusCode;
	}

	private static void attachSignature(HttpRequest.Builder builder, ModAuthManager.ModIdentity identity, String method, String path, byte[] bodyBytes) {
		ModAuthManager.ModIdentity.SignedHeaders headers = identity.sign(method, path, bodyBytes);
		builder.header("X-SkyMelloo-Uuid", headers.uuid())
				.header("X-SkyMelloo-Timestamp", headers.timestamp())
				.header("X-SkyMelloo-Nonce", headers.nonce())
				.header("X-SkyMelloo-Signature", headers.signature());
	}

	/** Only the path is signed, never the query string - none of these routes have sensitive/mutating query params. Prepends "/api" to match Express's req.path server-side. */
	private static String requestPath(String pathWithQuery) {
		int queryStart = pathWithQuery.indexOf('?');
		String pathOnly = queryStart < 0 ? pathWithQuery : pathWithQuery.substring(0, queryStart);
		return "/api" + pathOnly;
	}

	// ---- auth handshake ----

	public record ChallengeResult(String serverId, long serverTime) {
	}

	public static CompletableFuture<ChallengeResult> requestAuthChallenge() {
		return getJson("/mod/auth/challenge", null).thenApply(root ->
				new ChallengeResult(root.get("serverId").getAsString(), root.get("serverTime").getAsLong()));
	}

	public record SessionResult(long expiresAt) {
	}

	public static CompletableFuture<SessionResult> verifyAuthChallenge(String serverId, String username, String uuid, String publicKeyBase64) {
		JsonObject body = new JsonObject();
		body.addProperty("serverId", serverId);
		body.addProperty("username", username);
		body.addProperty("uuid", uuid);
		body.addProperty("publicKey", publicKeyBase64);
		return postJson("/mod/auth/verify", body, null)
				.thenApply(root -> new SessionResult(root.get("expiresAt").getAsLong()));
	}

	// ---- presence (cosmetics sync + role lookup) ----

	/** cosmetics: e.g. "halo:AA33FF" (custom color) or "cherryBlossom" (default color) - see PresenceManager. role is sky.melloo.me's server-resolved team role ("owner"/"admin"/"developer"/"moderator"), or null. */
	public record PresenceEntry(String uuid, String username, List<String> cosmetics, String role) {
	}

	public static CompletableFuture<Void> reportPresence(String uuid, String username, List<String> cosmetics, ModAuthManager.ModIdentity identity) {
		JsonObject body = new JsonObject();
		body.addProperty("uuid", uuid);
		body.addProperty("username", username);
		JsonArray cosmeticsArr = new JsonArray();
		for (String c : cosmetics) {
			cosmeticsArr.add(c);
		}
		body.add("cosmetics", cosmeticsArr);
		body.addProperty("status", "");
		body.addProperty("afk", false);
		body.addProperty("accountLinked", false);
		return postJson("/presence", body, identity).thenApply(root -> null);
	}

	public static CompletableFuture<List<PresenceEntry>> queryPresence(List<String> uuids, ModAuthManager.ModIdentity identity) {
		JsonObject body = new JsonObject();
		JsonArray uuidsArr = new JsonArray();
		for (String uuid : uuids) {
			uuidsArr.add(uuid);
		}
		body.add("uuids", uuidsArr);
		return postJson("/presence/query", body, identity).thenApply(root -> {
			List<PresenceEntry> result = new ArrayList<>();
			if (root.has("present") && root.get("present").isJsonArray()) {
				for (JsonElement el : root.getAsJsonArray("present")) {
					if (!el.isJsonObject()) {
						continue;
					}
					JsonObject entry = el.getAsJsonObject();
					if (!entry.has("uuid") || entry.get("uuid").isJsonNull()) {
						continue;
					}
					String uuid = entry.get("uuid").getAsString();
					String username = entry.has("username") && !entry.get("username").isJsonNull() ? entry.get("username").getAsString() : "";
					List<String> cosmetics = new ArrayList<>();
					if (entry.has("cosmetics") && entry.get("cosmetics").isJsonArray()) {
						for (JsonElement c : entry.getAsJsonArray("cosmetics")) {
							cosmetics.add(c.getAsString());
						}
					}
					String role = entry.has("role") && !entry.get("role").isJsonNull() ? entry.get("role").getAsString() : null;
					result.add(new PresenceEntry(uuid, username, cosmetics, role));
				}
			}
			return result;
		});
	}
}
