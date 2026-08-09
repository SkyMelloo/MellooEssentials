package com.melloo.mellooessentials.client;

import com.melloo.mellooessentials.client.cosmetics.CosmeticsRenderer;
import com.melloo.mellooessentials.client.gui.PlayerInfoHud;
import com.melloo.mellooessentials.client.gui.SettingsScreen;
import com.melloo.mellooessentials.client.gui.FpsMonitor;
import com.melloo.mellooessentials.client.party.PartyTracker;
import com.melloo.mellooessentials.client.social.HypixelLocationTracker;
import com.melloo.mellooessentials.client.social.PresenceManager;
import com.melloo.mellooessentials.client.util.HypixelDetector;
import com.melloo.mellooessentials.client.util.ServerPingMonitor;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class MellooEssentialsClient implements ClientModInitializer {
	public static final String MOD_ID = "mellooessentials";

	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(MOD_ID, "main")
	);

	private static KeyMapping openSettingsKey;

	@Override
	public void onInitializeClient() {
		openSettingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.mellooessentials.open_settings",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_H,
				CATEGORY
		));

		PartyTracker.init();
		HypixelLocationTracker.init();

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "player_info"), PlayerInfoHud.INSTANCE);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openSettingsKey.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new SettingsScreen(null));
				}
			}

			// Runs regardless of server - these measure the actual connection/game itself, not
			// anything Hypixel-specific.
			FpsMonitor.tick(client);
			ServerPingMonitor.tick(client);

			// Everything else is Hypixel-only - no reason to run party tracking/cosmetics/presence on
			// any other server.
			if (!HypixelDetector.isHypixel(client)) {
				return;
			}

			PartyTracker.tick();
			CosmeticsRenderer.tick(client);
			PresenceManager.tick(client);
		});
	}

	public static KeyMapping getOpenSettingsKey() {
		return openSettingsKey;
	}
}
