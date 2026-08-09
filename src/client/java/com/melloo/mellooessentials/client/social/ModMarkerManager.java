package com.melloo.mellooessentials.client.social;

import net.minecraft.client.Minecraft;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Prepends a small dye-sprite marker in front of a player's ENTIRE name (before any Hypixel rank
 * prefix, as far to the front as the component structure allows) - a real item icon, not a colored
 * unicode glyph stand-in, using the same inline sprite text-component support
 * ({@code ObjectContents}/{@code AtlasSprite}) this game version added. Moved here from
 * sky.melloo.ch's original AccountLinkedMarkerManager, generalized to take the sprite as a
 * parameter, and driven through a single mixin (this mod's own {@code EntityDisplayNameMixin}) so
 * only one marker ever gets added per player - never two mixins each independently prepending their
 * own icon, which would stack rather than override.
 * <p>
 * {@link #spriteOverride} is the extension point that makes the two-tier light-blue/pink behavior
 * possible without this mod knowing SkyMelloo exists: SkyMelloo (which depends on this mod, never
 * the other way around) registers a resolver via {@link #setSpriteOverride} at startup that returns
 * a different sprite for players it separately confirms are SkyMelloo users, or {@code null} to
 * leave this mod's own default untouched. No forced text color on either the sprite or its
 * connecting space - both use an explicit but colorless style ({@link Style#EMPTY}) purely to block
 * an earlier rank/name color code from bleeding onto the dye's own natural texture color.
 */
public final class ModMarkerManager {
	private static final String FALLBACK_GLYPH = "❖"; // used only if the sprite itself can't resolve

	private static volatile BiFunction<Player, Identifier, Identifier> spriteOverride = null;

	private ModMarkerManager() {
	}

	/** Registers a resolver that can swap out the sprite this mod would otherwise use for a given player - passed the player and this mod's own default sprite, returns a replacement or {@code null} to leave the default as-is. Only one resolver is ever active at a time (setting a new one replaces the last). */
	public static void setSpriteOverride(BiFunction<Player, Identifier, Identifier> resolver) {
		spriteOverride = resolver;
	}

	public static Component apply(Player player, Component original, Identifier defaultSpriteId) {
		Identifier spriteId = defaultSpriteId;
		BiFunction<Player, Identifier, Identifier> override = spriteOverride;
		if (override != null) {
			Identifier resolved = override.apply(player, defaultSpriteId);
			if (resolved != null) {
				spriteId = resolved;
			}
		}
		MutableComponent fallback = Component.literal(FALLBACK_GLYPH).setStyle(Style.EMPTY);
		MutableComponent icon = MutableComponent.create(new ObjectContents(new AtlasSprite(AtlasIds.ITEMS, spriteId), Optional.of(fallback)));
		icon.setStyle(Style.EMPTY);
		MutableComponent spacer = Component.literal(" ").setStyle(Style.EMPTY);
		return icon.append(spacer).append(original);
	}

	/** Whether {@code player} should get a marker at all - true for the local player, or anyone {@link PresenceManager} has detected reporting presence. */
	public static boolean isModUser(Player player) {
		return player == Minecraft.getInstance().player || PresenceManager.isModUser(player.getUUID());
	}
}
