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

	/** A single one-second retry on a plain timeout - see SkyMelloo's SkyMellooApiClient, same reasoning. */
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

	// -------------------------------------------------------------------------------------------
	// SkyMelloo Friends + relay chat - moved here from SkyMelloo's own SkyMellooApiClient (this was
	// always a SkyMelloo-branded feature name, but never actually keyed off the sky.melloo.me
	// account link - only the same anonymous per-launch ModIdentity this mod already uses for
	// presence, so it works identically here with zero server-side changes.
	// -------------------------------------------------------------------------------------------
	public record FriendEntry(String uuid, String username) {
	}

	public record FriendRequestEntry(String uuid, String username, long at) {
	}

	public record FriendsList(List<FriendEntry> friends, List<FriendRequestEntry> requests) {
	}

	public static CompletableFuture<FriendsList> fetchFriends(ModAuthManager.ModIdentity identity) {
		return getJson("/mod/friends", identity).thenApply(root -> {
			List<FriendEntry> friendsList = new ArrayList<>();
			if (root.has("friends") && root.get("friends").isJsonArray()) {
				for (JsonElement el : root.getAsJsonArray("friends")) {
					JsonObject o = el.getAsJsonObject();
					friendsList.add(new FriendEntry(o.get("uuid").getAsString(), o.get("username").getAsString()));
				}
			}
			List<FriendRequestEntry> requestsList = new ArrayList<>();
			if (root.has("requests") && root.get("requests").isJsonArray()) {
				for (JsonElement el : root.getAsJsonArray("requests")) {
					JsonObject o = el.getAsJsonObject();
					requestsList.add(new FriendRequestEntry(o.get("uuid").getAsString(), o.get("username").getAsString(), o.get("at").getAsLong()));
				}
			}
			return new FriendsList(friendsList, requestsList);
		});
	}

	/** {@code status} is one of "self", "already_friends", "accepted" (they'd already requested you back), "pending", or "limit". */
	public record FriendRequestResult(String username, String status) {
	}

	private static CompletableFuture<FriendRequestResult> friendAction(String path, String username, ModAuthManager.ModIdentity identity) {
		JsonObject body = new JsonObject();
		body.addProperty("username", username);
		return postJson(path, body, identity)
				.thenApply(root -> new FriendRequestResult(
						root.has("username") && !root.get("username").isJsonNull() ? root.get("username").getAsString() : username,
						root.has("status") && !root.get("status").isJsonNull() ? root.get("status").getAsString() : null));
	}

	public static CompletableFuture<FriendRequestResult> sendFriendRequest(String username, ModAuthManager.ModIdentity identity) {
		return friendAction("/mod/friends/request", username, identity);
	}

	public static CompletableFuture<FriendRequestResult> acceptFriendRequest(String username, ModAuthManager.ModIdentity identity) {
		return friendAction("/mod/friends/accept", username, identity);
	}

	public static CompletableFuture<FriendRequestResult> declineFriendRequest(String username, ModAuthManager.ModIdentity identity) {
		return friendAction("/mod/friends/decline", username, identity);
	}

	public static CompletableFuture<FriendRequestResult> removeFriend(String username, ModAuthManager.ModIdentity identity) {
		return friendAction("/mod/friends/remove", username, identity);
	}

	/** Sends a DM to a friend by username - the server rejects it (403) unless the two accounts are already confirmed friends. */
	public static CompletableFuture<Boolean> sendRelayMessage(String toUsername, String text, ModAuthManager.ModIdentity identity) {
		JsonObject body = new JsonObject();
		body.addProperty("toUsername", toUsername);
		body.addProperty("text", text);
		return postJson("/mod/relay/message", body, identity)
				.thenApply(root -> true)
				.exceptionally(error -> false);
	}

	/** Broadcasts to a caller-resolved list of party-member UUIDs (the server has no visibility into real Hypixel parties, so this trusts whichever roster the mod itself resolved). */
	public static CompletableFuture<Boolean> sendRelayPartyMessage(List<String> toUuids, String text, ModAuthManager.ModIdentity identity) {
		JsonObject body = new JsonObject();
		JsonArray uuidsArr = new JsonArray();
		for (String uuid : toUuids) {
			uuidsArr.add(uuid);
		}
		body.add("toUuids", uuidsArr);
		body.addProperty("text", text);
		return postJson("/mod/relay/party", body, identity)
				.thenApply(root -> true)
				.exceptionally(error -> false);
	}

	/** One relayed message waiting in the inbox - {@code scope} is "dm" or "party". */
	public record RelayMessage(String fromUuid, String fromUsername, String text, String scope) {
	}

	/** Drains (not peeks) everything currently queued for this account - polled every few seconds by RelayChatManager. */
	public static CompletableFuture<List<RelayMessage>> fetchRelayInbox(ModAuthManager.ModIdentity identity) {
		return getJson("/mod/relay/inbox", identity).thenApply(root -> {
			List<RelayMessage> result = new ArrayList<>();
			if (root.has("messages") && root.get("messages").isJsonArray()) {
				for (JsonElement el : root.getAsJsonArray("messages")) {
					JsonObject o = el.getAsJsonObject();
					result.add(new RelayMessage(
							o.get("from").getAsString(),
							o.has("fromUsername") && !o.get("fromUsername").isJsonNull() ? o.get("fromUsername").getAsString() : "?",
							o.get("text").getAsString(),
							o.has("scope") && !o.get("scope").isJsonNull() ? o.get("scope").getAsString() : "dm"));
				}
			}
			return result;
		}).exceptionally(error -> List.of());
	}

	// -------------------------------------------------------------------------------------------
	// Encountered staff - moved here from SkyMelloo's own SkyMellooApiClient/StaffEncounterTracker,
	// same zero-server-change reasoning as Friends above.
	// -------------------------------------------------------------------------------------------

	/** One nearby player, as seen in the tab list - all the server needs to check them against the staff/owner roster. */
	public record StaffCheckEntry(String uuid, String username) {
	}

	/** Reports everyone currently visible in the tab list so the server can record an encounter for any of them that resolve to a real staff/owner role - fire-and-forget. */
	public static CompletableFuture<Void> reportStaffEncounters(List<StaffCheckEntry> players, ModAuthManager.ModIdentity identity) {
		JsonObject body = new JsonObject();
		JsonArray playersArr = new JsonArray();
		for (StaffCheckEntry p : players) {
			JsonObject entry = new JsonObject();
			entry.addProperty("uuid", p.uuid());
			entry.addProperty("username", p.username());
			playersArr.add(entry);
		}
		body.add("players", playersArr);
		return postJson("/mod/staff-encounters", body, identity).thenApply(root -> null);
	}

	/** One staff/owner member this account has ever been seen alongside, per the server's own encounter log - see the "/mellooessentials hitstaff" command. websiteDisplayName is null when that staff uuid has no linked sky.melloo.me website account. */
	public record StaffEncounterEntry(String uuid, String username, String role, long firstSeenMillis, long lastSeenMillis, String websiteDisplayName) {
	}

	public static CompletableFuture<List<StaffEncounterEntry>> fetchStaffEncounters(ModAuthManager.ModIdentity identity) {
		return getJson("/mod/staff-encounters", identity).thenApply(root -> {
			List<StaffEncounterEntry> result = new ArrayList<>();
			if (root.has("encounters") && root.get("encounters").isJsonArray()) {
				for (JsonElement el : root.getAsJsonArray("encounters")) {
					if (!el.isJsonObject()) {
						continue;
					}
					JsonObject o = el.getAsJsonObject();
					result.add(new StaffEncounterEntry(
							o.get("uuid").getAsString(),
							o.get("username").getAsString(),
							o.get("role").getAsString(),
							o.get("firstSeenMillis").getAsLong(),
							o.get("lastSeenMillis").getAsLong(),
							o.has("websiteDisplayName") && !o.get("websiteDisplayName").isJsonNull() ? o.get("websiteDisplayName").getAsString() : null));
				}
			}
			return result;
		}).exceptionally(error -> List.of());
	}
}
