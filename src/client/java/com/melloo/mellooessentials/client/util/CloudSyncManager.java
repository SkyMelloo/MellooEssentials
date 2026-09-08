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

// Syncs MellooEssentials settings to sky.melloo.me - requires cloudSyncEnabled and a linked
// account. Cloud is unconditionally authoritative on join, no timestamp/content-diff comparison.
public final class CloudSyncManager {
	// Color serialized as a plain RGB int - reflecting into its private fields can throw under the JDK module system.
	private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(Color.class, (com.google.gson.JsonSerializer<Color>) (src, type, ctx) ->
					new com.google.gson.JsonPrimitive(src.getRGB()))
			.registerTypeAdapter(Color.class, (com.google.gson.JsonDeserializer<Color>) (json, type, ctx) ->
					new Color(json.getAsInt(), true))
			.create();

	// Once per launch - the JOIN event also fires on Hypixel's own internal server-hops.
	private static volatile boolean syncAttempted = false;

	private CloudSyncManager() {
	}

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

	// If nothing's been pushed yet, bootstraps the cloud from this device's current settings instead.
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

	// Copies every matching public field via reflection, avoiding hand-listing every settings field.
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
