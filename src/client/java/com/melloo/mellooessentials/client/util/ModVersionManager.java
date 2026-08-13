package com.melloo.mellooessentials.client.util;

import com.melloo.mellooessentials.client.MellooEssentialsClient;
import com.melloo.mellooessentials.client.api.ApiClient;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
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
 * Version/integrity check for THIS mod, and - since Fabric Loader's mod-container registry is
 * global, not scoped to whichever mod queries it - for SkyMelloo too, if it's installed. Used to be
 * two entirely separate copies of this system (one per mod), which meant two independent join-time
 * checks and two independent "unofficial build" chat notices firing back to back. Consolidated here
 * since this mod already has no dependency direction issue either way (SkyMelloo depends on this
 * mod, never the other way around) - SkyMelloo's own "/sm version"/"/sm legal" now just read the
 * SkyMelloo-prefixed getters below instead of running a duplicate check of their own.
 */
public final class ModVersionManager {
	private static volatile boolean selfCheckStarted = false;
	private static volatile boolean selfCompatible = true;
	private static volatile String selfLocalVersion = "0.0.0";
	private static volatile String selfPublicVersion = "unreleased";
	private static volatile String selfJarHash = null;
	private static volatile ApiClient.VersionCheckResult selfLastResult = null;
	private static final long MANUAL_CHECK_COOLDOWN_MS = 3000;
	private static volatile long lastManualCheckMillis = 0;

	private static volatile boolean skyMellooCheckStarted = false;
	private static volatile boolean skyMellooCompatible = true;
	private static volatile String skyMellooLocalVersion = "0.0.0";
	private static volatile String skyMellooPublicVersion = "unreleased";
	private static volatile String skyMellooJarHash = null;
	private static volatile ApiClient.VersionCheckResult skyMellooLastResult = null;
	private static volatile long lastManualCheckMillisSkyMelloo = 0;

	private ModVersionManager() {
	}

	public static String getLocalVersion() {
		return selfLocalVersion;
	}

	public static String getPublicVersion() {
		return selfPublicVersion;
	}

	public static String getLocalJarHash() {
		return selfJarHash;
	}

	/** {@code null} until the one join-time check actually completes (or if it failed outright). */
	public static ApiClient.VersionCheckResult getLastResult() {
		return selfLastResult;
	}

	public static String getSkyMellooLocalVersion() {
		return skyMellooLocalVersion;
	}

	public static String getSkyMellooPublicVersion() {
		return skyMellooPublicVersion;
	}

	public static String getSkyMellooJarHash() {
		return skyMellooJarHash;
	}

	/** {@code null} until the one join-time check actually completes, if SkyMelloo isn't installed, or if it failed outright. */
	public static ApiClient.VersionCheckResult getSkyMellooLastResult() {
		return skyMellooLastResult;
	}

	/** Fires a fresh check right now, for "/mes version". */
	public static void checkNow(java.util.function.Consumer<ApiClient.VersionCheckResult> onResult, java.util.function.Consumer<Long> onCooldown) {
		checkNowShared(selfLocalVersion, selfJarHash, ApiClient::checkVersion, onResult, onCooldown,
				System.currentTimeMillis(), lastManualCheckMillis, millis -> lastManualCheckMillis = millis,
				result -> {
					selfLastResult = result;
					selfCompatible = result.compatible();
				});
	}

	/** Fires a fresh check right now, for SkyMelloo's own "/sm version" - same idea as {@link #checkNow}, just against SkyMelloo's route/cached values instead of this mod's own. */
	public static void checkSkyMellooNow(java.util.function.Consumer<ApiClient.VersionCheckResult> onResult, java.util.function.Consumer<Long> onCooldown) {
		checkNowShared(skyMellooLocalVersion, skyMellooJarHash, ApiClient::checkVersionForSkyMelloo, onResult, onCooldown,
				System.currentTimeMillis(), lastManualCheckMillisSkyMelloo, millis -> lastManualCheckMillisSkyMelloo = millis,
				result -> {
					skyMellooLastResult = result;
					skyMellooCompatible = result.compatible();
				});
	}

	private static void checkNowShared(String version, String jarHash,
			java.util.function.BiFunction<String, String, java.util.concurrent.CompletableFuture<ApiClient.VersionCheckResult>> checker,
			java.util.function.Consumer<ApiClient.VersionCheckResult> onResult, java.util.function.Consumer<Long> onCooldown,
			long now, long lastMillis, java.util.function.LongConsumer setLastMillis, java.util.function.Consumer<ApiClient.VersionCheckResult> onStored) {
		long remaining = MANUAL_CHECK_COOLDOWN_MS - (now - lastMillis);
		if (remaining > 0) {
			onCooldown.accept((remaining + 999) / 1000);
			return;
		}
		setLastMillis.accept(now);
		checker.apply(version, jarHash).whenComplete((result, error) -> Minecraft.getInstance().execute(() -> {
			if (error != null || result == null) {
				onResult.accept(null);
				return;
			}
			onStored.accept(result);
			onResult.accept(result);
		}));
	}

	public static void checkOnce(Minecraft client) {
		checkSelfOnce(client);
		checkSkyMellooOnce(client);
	}

	private static void checkSelfOnce(Minecraft client) {
		if (selfCheckStarted || client.player == null) {
			return;
		}
		selfCheckStarted = true;
		var containerOpt = FabricLoader.getInstance().getModContainer(MellooEssentialsClient.MOD_ID);
		String version = containerOpt
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse("0.0.0");
		selfLocalVersion = version;
		selfPublicVersion = containerOpt
				.map(container -> container.getMetadata().getCustomValue("mellooessentials:publicVersion"))
				.filter(value -> value != null && value.getType() == net.fabricmc.loader.api.metadata.CustomValue.CvType.STRING)
				.map(net.fabricmc.loader.api.metadata.CustomValue::getAsString)
				.orElse("unreleased");
		String jarHash = containerOpt.map(container -> computeJarHash(container, "com", "melloo", "mellooessentials")).orElse(null);
		selfJarHash = jarHash;
		ApiClient.checkVersion(version, jarHash).whenComplete((result, error) -> Minecraft.getInstance().execute(() -> {
			if (error != null) {
				return;
			}
			selfLastResult = result;
			selfCompatible = result.compatible();
			// Purely informational - an unverified build gets a one-time handshake chat notice,
			// nothing is ever disabled.
			if (!result.integrityOk() && client.player != null) {
				String maintainer = result.maintainerUsername() != null ? result.maintainerUsername() : "the maintainer";
				client.player.sendSystemMessage(ChatUtil.prefixed(
						Component.translatable("mellooessentials.chat.version.unofficial_build", "MellooEssentials", maintainer)));
			} else if (!selfCompatible && client.player != null) {
				client.player.sendSystemMessage(ChatUtil.prefixed("§e" + result.message()));
			} else if (selfCompatible && !result.upToDate() && client.player != null && result.updateAvailableMessage() != null) {
				client.player.sendSystemMessage(ChatUtil.prefixed("§e" + result.updateAvailableMessage()));
			}
		}));
	}

	/** Same as {@link #checkSelfOnce} but against SkyMelloo's own mod container/route - silently does nothing if SkyMelloo isn't installed at all. */
	private static void checkSkyMellooOnce(Minecraft client) {
		if (skyMellooCheckStarted || client.player == null) {
			return;
		}
		var containerOpt = FabricLoader.getInstance().getModContainer("skymelloo");
		if (containerOpt.isEmpty()) {
			return;
		}
		skyMellooCheckStarted = true;
		ModContainer container = containerOpt.get();
		String version = container.getMetadata().getVersion().getFriendlyString();
		skyMellooLocalVersion = version;
		skyMellooPublicVersion = java.util.Optional.ofNullable(container.getMetadata().getCustomValue("skymelloo:publicVersion"))
				.filter(value -> value.getType() == net.fabricmc.loader.api.metadata.CustomValue.CvType.STRING)
				.map(net.fabricmc.loader.api.metadata.CustomValue::getAsString)
				.orElse("unreleased");
		String jarHash = computeJarHash(container, "com", "melloo", "skymelloo");
		skyMellooJarHash = jarHash;
		ApiClient.checkVersionForSkyMelloo(version, jarHash).whenComplete((result, error) -> Minecraft.getInstance().execute(() -> {
			if (error != null) {
				return;
			}
			skyMellooLastResult = result;
			skyMellooCompatible = result.compatible();
			if (!result.integrityOk() && client.player != null) {
				String maintainer = result.maintainerUsername() != null ? result.maintainerUsername() : "the maintainer";
				client.player.sendSystemMessage(ChatUtil.prefixed(
						Component.translatable("mellooessentials.chat.version.unofficial_build", "SkyMelloo", maintainer)));
			} else if (!skyMellooCompatible && client.player != null) {
				client.player.sendSystemMessage(ChatUtil.prefixed("§e" + result.message()));
			} else if (skyMellooCompatible && !result.upToDate() && client.player != null && result.updateAvailableMessage() != null) {
				client.player.sendSystemMessage(ChatUtil.prefixed("§e" + result.updateAvailableMessage()));
			}
		}));
	}

	/**
	 * Lowercase hex SHA-256 of {@code container}'s own compiled classes under the given package path
	 * - {@code null} if that can't be determined safely (Gradle's runClient dev environment before
	 * anything's compiled; or an origin shape this doesn't confidently recognize). Works for ANY
	 * loaded mod's container, not just this mod's own, since Fabric Loader's registry is global -
	 * that's what lets this mod hash SkyMelloo's classes too, not just its own.
	 * <p>
	 * Scoped to just the given package on purpose - the goal is verifying that mod's own code
	 * specifically, not anything Lunar Client bundles alongside it. Opens the packaged jar as its own
	 * zip filesystem first if the resolved root is a single file, so this reads the REAL compiled
	 * bytecode regardless of whatever container Lunar wraps it in. Broadly catches {@code Throwable},
	 * not just the specific exceptions expected here - this runs on the render thread on every launch
	 * and must never be able to crash the game again; a missing hash is only ever treated as
	 * "unknown" by the backend, never as "invalid".
	 */
	private static String computeJarHash(ModContainer container, String... packageSegments) {
		try {
			List<Path> roots = container.getRootPaths();
			if (roots.size() != 1) {
				// Not the plain single-jar shape this is built for - rather than guess how to combine
				// multiple roots, just report unknown.
				return null;
			}
			Path root = roots.get(0);
			if (Files.isRegularFile(root)) {
				try (FileSystem zipFs = FileSystems.newFileSystem(root)) {
					return hashClassesUnder(zipFs.getPath(packageSegments[0], java.util.Arrays.copyOfRange(packageSegments, 1, packageSegments.length)));
				}
			}
			if (Files.isDirectory(root)) {
				Path packageRoot = root;
				for (String segment : packageSegments) {
					packageRoot = packageRoot.resolve(segment);
				}
				return hashClassesUnder(packageRoot);
			}
			return null;
		} catch (Throwable e) {
			return null;
		}
	}

	/** Hashes every {@code .class} file under {@code packageRoot} in a stable (sorted, relative-path) order, or {@code null} if there's nothing there. */
	private static String hashClassesUnder(Path packageRoot) throws Exception {
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
