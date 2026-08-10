package com.melloo.mellooessentials.client.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.melloo.mellooessentials.client.api.ApiClient;
import com.melloo.mellooessentials.client.api.ModAuthManager;
import com.melloo.mellooessentials.client.config.EssentialsConfig;
import net.minecraft.client.Minecraft;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.CompletableFuture;

/**
 * Syncs MellooEssentials settings (HUD positions, cosmetics) to sky.melloo.me - same architecture
 * and reasoning as SkyMelloo's own {@code CloudSyncManager}: requires both {@code cloudSyncEnabled}
 * (on by default) and a linked Minecraft account, and cloud is unconditionally authoritative on
 * join (no timestamp/content-diff comparison - see SkyMelloo's own doc comment for the full history
 * of why that approach was abandoned). Built after a live report that HUD positions set in one
 * Lunar Client profile didn't show up in another - each profile has its own separate config file on
 * disk, so only a real sync (not a local-file trick) actually fixes that.
 */
public final class CloudSyncManager {
	// Color fields are serialized as a plain RGB int via a custom adapter, matching EssentialsConfig's
	// own local-save Gson instance - reflecting into java.awt.Color's private fields directly can
	// throw InaccessibleObjectException under the JDK module system.
	private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(Color.class, (com.google.gson.JsonSerializer<Color>) (src, type, ctx) ->
					new com.google.gson.JsonPrimitive(src.getRGB()))
			.registerTypeAdapter(Color.class, (com.google.gson.JsonDeserializer<Color>) (json, type, ctx) ->
					new Color(json.getAsInt(), true))
			.create();

	// Attempted once per game launch - the join event this would otherwise reset on also fires on
	// Hypixel's own internal server-hops, so resetting there would mean re-running this constantly.
	private static volatile boolean syncAttempted = false;

	private CloudSyncManager() {
	}

	/** Called every tick - resolves link status itself (a fresh fetch) rather than needing a cached copy, same as SkyMelloo's own pullIfNeeded. */
	public static void pullIfNeeded(Minecraft client) {
		EssentialsConfig config = EssentialsConfig.get();
		if (syncAttempted || !config.cloudSyncEnabled || client.player == null) {
			return;
		}
		syncAttempted = true;
		ModAuthManager.getIdentity(client).thenCompose(identity ->
				ApiClient.fetchPermissions(identity).thenCompose(perms -> {
					if (!Boolean.TRUE.equals(perms.get("accountLinked"))) {
						return CompletableFuture.<Void>completedFuture(null);
					}
					return reconcile(identity);
				})
		).exceptionally(error -> null);
	}

	/** Pulls whatever's in the cloud and applies it unconditionally - or, if nothing's been pushed there yet at all, bootstraps the cloud from this device's current settings instead. */
	private static CompletableFuture<Void> reconcile(ModAuthManager.ModIdentity identity) {
		return ApiClient.fetchCloudSettings(identity).thenAccept(result ->
				Minecraft.getInstance().execute(() -> {
					if (result == null) {
						pushWithIdentity(identity);
						return;
					}
					applySettings(result.settings());
				})
		);
	}

	/** "Pull Now" button in the Cloud tab - same unconditional pull as the automatic join-time one, just triggered on demand. */
	public static void forcePull(Minecraft client, Runnable onApplied) {
		if (client.player == null) {
			return;
		}
		ModAuthManager.getIdentity(client).thenCompose(ApiClient::fetchCloudSettings).whenComplete((result, error) ->
				Minecraft.getInstance().execute(() -> {
					if (result != null) {
						applySettings(result.settings());
						onApplied.run();
					}
				})
		);
	}

	/** Uploads the current settings - called when the settings/HUD editor screen closes, so most changes sync near-immediately. */
	public static void push(Minecraft client) {
		EssentialsConfig config = EssentialsConfig.get();
		if (!config.cloudSyncEnabled || client.player == null) {
			return;
		}
		ModAuthManager.getIdentity(client).thenCompose(identity ->
				ApiClient.fetchPermissions(identity).thenAccept(perms -> {
					if (Boolean.TRUE.equals(perms.get("accountLinked"))) {
						pushWithIdentity(identity);
					}
				})
		);
	}

	private static void pushWithIdentity(ModAuthManager.ModIdentity identity) {
		JsonObject json = GSON.toJsonTree(EssentialsConfig.get(), EssentialsConfig.class).getAsJsonObject();
		ApiClient.pushCloudSettings(identity, json);
	}

	/** Copies every matching public field from the parsed cloud blob onto the live config via reflection - avoids hand-listing every one of the ~30 cosmetic/HUD fields. */
	private static void applySettings(JsonObject settingsJson) {
		EssentialsConfig parsed;
		try {
			parsed = GSON.fromJson(settingsJson, EssentialsConfig.class);
		} catch (Exception e) {
			return;
		}
		if (parsed == null) {
			return;
		}
		EssentialsConfig config = EssentialsConfig.get();
		for (Field field : EssentialsConfig.class.getFields()) {
			int mods = field.getModifiers();
			if (Modifier.isStatic(mods) || Modifier.isFinal(mods)) {
				continue;
			}
			try {
				field.set(config, field.get(parsed));
			} catch (IllegalAccessException ignored) {
				// all matched fields here are public instance fields - shouldn't happen
			}
		}
		EssentialsConfig.save();
	}
}
