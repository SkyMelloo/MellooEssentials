package com.melloo.mellooessentials.client.social;

import net.minecraft.client.Minecraft;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.UUID;

// Prepends a real item-sprite marker in front of a player's name. Keyed by UUID (not a loaded
// Player entity) since the tab list can show players outside render distance.
public final class ModMarkerManager {
	private static final String FALLBACK_GLYPH = "❖"; // used only if the sprite can't resolve

	private ModMarkerManager() {
	}

	public static Component apply(UUID uuid, Component original, Identifier spriteId) {
		MutableComponent fallback = Component.literal(FALLBACK_GLYPH).setStyle(Style.EMPTY);
		MutableComponent icon = MutableComponent.create(new ObjectContents(new AtlasSprite(AtlasIds.ITEMS, spriteId), Optional.of(fallback)));
		icon.setStyle(Style.EMPTY);
		// A thin space (U+2009), not a regular one - a flush icon/name gap read too tight.
		MutableComponent spacer = Component.literal(" ").setStyle(Style.EMPTY);
		return icon.append(spacer).append(original);
	}

	public static boolean isModUser(UUID uuid) {
		var local = Minecraft.getInstance().player;
		return (local != null && local.getUUID().equals(uuid)) || PresenceManager.isModUser(uuid);
	}
}
