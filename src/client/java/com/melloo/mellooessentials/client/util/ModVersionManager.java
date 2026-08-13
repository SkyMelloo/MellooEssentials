package com.melloo.mellooessentials.client.util;

import com.melloo.mellooessentials.client.MellooEssentialsClient;
import com.melloo.mellooessentials.client.api.ApiClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.List;

/**
 * Same version/integrity check as SkyMelloo's own ModVersionManager, ported over so
 * MellooEssentials-only installs (no SkyMelloo) get the same "/me version" and "/me legal"
 * commands and the same one-time unofficial-build notice - see SkyMelloo's ModVersionManager for
 * the full reasoning on why the jar hash is scoped to just this mod's own package.
 */
public final class ModVersionManager {
	private static volatile boolean checkStarted = false;
	private static volatile boolean compatible = true;
	private static volatile String localVersion = "0.0.0";
	private static volatile String publicVersion = "unreleased";
	private static volatile String localJarHash = null;
	private static volatile ApiClient.VersionCheckResult lastResult = null;
	private static final long MANUAL_CHECK_COOLDOWN_MS = 3000;
	private static volatile long lastManualCheckMillis = 0;

	private ModVersionManager() {
	}

	public static String getLocalVersion() {
		return localVersion;
	}

	public static String getPublicVersion() {
		return publicVersion;
	}

	public static String getLocalJarHash() {
		return localJarHash;
	}

	/** {@code null} until the one join-time check actually completes (or if it failed outright). */
	public static ApiClient.VersionCheckResult getLastResult() {
		return lastResult;
	}

	/** Fires a fresh check right now, for "/me version" - see SkyMelloo's own checkNow for the full reasoning. */
	public static void checkNow(java.util.function.Consumer<ApiClient.VersionCheckResult> onResult, java.util.function.Consumer<Long> onCooldown) {
		long now = System.currentTimeMillis();
		long remaining = MANUAL_CHECK_COOLDOWN_MS - (now - lastManualCheckMillis);
		if (remaining > 0) {
			onCooldown.accept((remaining + 999) / 1000);
			return;
		}
		lastManualCheckMillis = now;
		ApiClient.checkVersion(localVersion, localJarHash).whenComplete((result, error) -> Minecraft.getInstance().execute(() -> {
			if (error != null || result == null) {
				onResult.accept(null);
				return;
			}
			lastResult = result;
			compatible = result.compatible();
			onResult.accept(result);
		}));
	}

	public static void checkOnce(Minecraft client) {
		if (checkStarted || client.player == null) {
			return;
		}
		checkStarted = true;
		var containerOpt = FabricLoader.getInstance().getModContainer(MellooEssentialsClient.MOD_ID);
		String version = containerOpt
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse("0.0.0");
		localVersion = version;
		publicVersion = containerOpt
				.map(container -> container.getMetadata().getCustomValue("mellooessentials:publicVersion"))
				.filter(value -> value != null && value.getType() == net.fabricmc.loader.api.metadata.CustomValue.CvType.STRING)
				.map(net.fabricmc.loader.api.metadata.CustomValue::getAsString)
				.orElse("unreleased");
		String jarHash = computeOwnJarHash();
		localJarHash = jarHash;
		ApiClient.checkVersion(version, jarHash).whenComplete((result, error) -> Minecraft.getInstance().execute(() -> {
			if (error != null) {
				return;
			}
			lastResult = result;
			compatible = result.compatible();
			// Purely informational - an unverified build gets a one-time handshake chat notice,
			// nothing is ever disabled.
			if (!result.integrityOk() && client.player != null) {
				String maintainer = result.maintainerUsername() != null ? result.maintainerUsername() : "the maintainer";
				client.player.sendSystemMessage(ChatUtil.prefixed(
						Component.translatable("mellooessentials.chat.version.unofficial_build", maintainer)));
			} else if (!compatible && client.player != null) {
				client.player.sendSystemMessage(ChatUtil.prefixed("§e" + result.message()));
			} else if (compatible && !result.upToDate() && client.player != null && result.updateAvailableMessage() != null) {
				client.player.sendSystemMessage(ChatUtil.prefixed("§e" + result.updateAvailableMessage()));
			}
		}));
	}

	/** Lowercase hex SHA-256 of this mod's OWN compiled classes - see SkyMelloo's ModVersionManager#computeOwnJarHash for the full reasoning on scope/Lunar-wrapping/error handling. */
	private static String computeOwnJarHash() {
		try {
			var containerOpt = FabricLoader.getInstance().getModContainer(MellooEssentialsClient.MOD_ID);
			if (containerOpt.isEmpty()) {
				return null;
			}
			List<Path> roots = containerOpt.get().getRootPaths();
			if (roots.size() != 1) {
				return null;
			}
			Path root = roots.get(0);
			if (Files.isRegularFile(root)) {
				try (FileSystem zipFs = FileSystems.newFileSystem(root)) {
					return hashOwnClassesUnder(zipFs.getPath("com", "melloo", "mellooessentials"));
				}
			}
			if (Files.isDirectory(root)) {
				return hashOwnClassesUnder(root.resolve("com").resolve("melloo").resolve("mellooessentials"));
			}
			return null;
		} catch (Throwable e) {
			return null;
		}
	}

	private static String hashOwnClassesUnder(Path packageRoot) throws Exception {
		if (!Files.isDirectory(packageRoot)) {
			return null;
		}
		List<Path> classFiles;
		try (var walk = Files.walk(packageRoot)) {
			classFiles = walk.filter(p -> p.toString().endsWith(".class"))
					.sorted(Comparator.comparing(p -> packageRoot.relativize(p).toString()))
					.toList();
		}
		if (classFiles.isEmpty()) {
			return null;
		}
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		for (Path file : classFiles) {
			digest.update(packageRoot.relativize(file).toString().getBytes(StandardCharsets.UTF_8));
			digest.update(Files.readAllBytes(file));
		}
		StringBuilder hex = new StringBuilder();
		for (byte b : digest.digest()) {
			hex.append(String.format("%02x", b));
		}
		return hex.toString();
	}
}
