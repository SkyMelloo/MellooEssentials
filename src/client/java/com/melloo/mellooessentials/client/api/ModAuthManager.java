package com.melloo.mellooessentials.client.api;

import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Proves to sky.melloo.me that a request genuinely comes from a live, logged-in Minecraft client
 * for a specific account, via the exact same joinServer/hasJoined handshake sky.melloo.ch's
 * ModAuthManager uses - only proves "a real Mojang account is behind this", nothing tied to a
 * Discord/website account, so this needs no login/account system of its own at all. A fresh
 * in-memory-only Ed25519 keypair proves every subsequent request instead of a reusable secret.
 */
public final class ModAuthManager {
	private static final Logger LOGGER = LoggerFactory.getLogger("MellooEssentials/ModAuthManager");
	private static final long REFRESH_MARGIN_MS = 5 * 60 * 1000;

	private static volatile CompletableFuture<ModIdentity> identityFuture = null;
	private static volatile long identityExpiresAt = 0;
	private static volatile KeyPair ephemeralKeyPair = null;

	private ModAuthManager() {
	}

	public record ModIdentity(String uuid, String username, PrivateKey signingKey, long clockOffsetMs) {
		private static final SecureRandom RANDOM = new SecureRandom();

		public record SignedHeaders(String uuid, String timestamp, String nonce, String signature) {
		}

		public SignedHeaders sign(String method, String path, byte[] bodyBytes) {
			long timestamp = System.currentTimeMillis() + clockOffsetMs;
			byte[] nonceBytes = new byte[16];
			RANDOM.nextBytes(nonceBytes);
			String nonce = HexFormat.of().formatHex(nonceBytes);
			String bodyHash = HexFormat.of().formatHex(sha256(bodyBytes));
			String message = String.join("\n", uuid, method, path, String.valueOf(timestamp), nonce, bodyHash);
			try {
				Signature signer = Signature.getInstance("Ed25519");
				signer.initSign(signingKey);
				signer.update(message.getBytes(StandardCharsets.UTF_8));
				String signatureBase64 = Base64.getEncoder().encodeToString(signer.sign());
				return new SignedHeaders(uuid, String.valueOf(timestamp), nonce, signatureBase64);
			} catch (GeneralSecurityException e) {
				throw new RuntimeException(e);
			}
		}

		private static byte[] sha256(byte[] data) {
			try {
				return MessageDigest.getInstance("SHA-256").digest(data);
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static synchronized CompletableFuture<ModIdentity> getIdentity(Minecraft client) {
		if (identityFuture != null && (!identityFuture.isDone() || System.currentTimeMillis() < identityExpiresAt - REFRESH_MARGIN_MS)) {
			return identityFuture;
		}
		identityFuture = authenticate(client);
		return identityFuture;
	}

	private static synchronized KeyPair ephemeralKeyPair() {
		if (ephemeralKeyPair == null) {
			try {
				ephemeralKeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
		}
		return ephemeralKeyPair;
	}

	private static CompletableFuture<ModIdentity> authenticate(Minecraft client) {
		User user = client.getUser();
		if (user == null || user.getAccessToken() == null || user.getAccessToken().isBlank()) {
			return CompletableFuture.failedFuture(new IllegalStateException("No Minecraft session available"));
		}
		String username = user.getName();
		String uuid = user.getProfileId().toString().replace("-", "").toLowerCase(Locale.ROOT);
		MinecraftSessionService sessionService = client.services().sessionService();
		KeyPair keyPair = ephemeralKeyPair();
		String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

		return ApiClient.requestAuthChallenge()
				.thenComposeAsync(challenge -> {
					long clockOffsetMs = challenge.serverTime() - System.currentTimeMillis();
					return CompletableFuture.supplyAsync(() -> {
						try {
							sessionService.joinServer(user.getProfileId(), user.getAccessToken(), challenge.serverId());
						} catch (AuthenticationException e) {
							throw new CompletionException(e);
						}
						return clockOffsetMs;
					}).thenCompose(offset -> ApiClient
							.verifyAuthChallenge(challenge.serverId(), username, uuid, publicKeyBase64)
							.thenApply(result -> {
								identityExpiresAt = result.expiresAt();
								return new ModIdentity(uuid, username, keyPair.getPrivate(), offset);
							}));
				})
				.exceptionally(error -> {
					LOGGER.debug("Mod auth failed: " + error.getMessage());
					identityFuture = null;
					throw new CompletionException(error);
				});
	}
}
