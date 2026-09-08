package com.melloo.mellooessentials.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Fetches a flat player-face avatar image over HTTP (mc-heads.net) and registers it as a real
// Minecraft texture, keyed by UUID. Works for any player, online or not.
public final class RemoteFaceTextureCache {
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
	private static final Map<UUID, Identifier> resolved = new ConcurrentHashMap<>();
	private static final Map<UUID, Boolean> inFlight = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> failedAt = new ConcurrentHashMap<>();
	private static final long RETRY_AFTER_MS = 60_000;

	private RemoteFaceTextureCache() {
	}

	// Null if not resolved yet; kicks off a fetch, so just poll again next frame.
	public static Identifier get(UUID uuid) {
		if (uuid == null) {
			return null;
		}
		Identifier cached = resolved.get(uuid);
		if (cached != null) {
			return cached;
		}
		if (Boolean.TRUE.equals(inFlight.get(uuid))) {
			return null;
		}
		Long lastFail = failedAt.get(uuid);
		if (lastFail != null && System.currentTimeMillis() - lastFail < RETRY_AFTER_MS) {
			return null;
		}
		fetch(uuid);
		return null;
	}

	private static void fetch(UUID uuid) {
		inFlight.put(uuid, true);
		String dashless = uuid.toString().replace("-", "");
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://mc-heads.net/avatar/" + dashless + "/32"))
				.timeout(Duration.ofSeconds(8))
				.GET().build();
		HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
				.thenAccept(response -> {
					if (response.statusCode() != 200) {
						throw new RuntimeException("HTTP " + response.statusCode());
					}
					NativeImage image;
					try {
						image = NativeImage.read(response.body());
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
					Minecraft.getInstance().execute(() -> {
						DynamicTexture texture = new DynamicTexture(() -> "mellooessentials-face-" + dashless, image);
						Identifier id = Identifier.fromNamespaceAndPath("mellooessentials", "friend_face_" + dashless);
						Minecraft.getInstance().getTextureManager().register(id, texture);
						resolved.put(uuid, id);
						inFlight.remove(uuid);
					});
				})
				.exceptionally(error -> {
					failedAt.put(uuid, System.currentTimeMillis());
					inFlight.remove(uuid);
					return null;
				});
	}
}
