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

/**
 * Prepends a small dye-sprite marker in front of a player's ENTIRE name (before any Hypixel rank
 * prefix, as far to the front as the component structure allows) - a real item icon, not a colored
 * unicode glyph stand-in, using the same inline sprite text-component support
 * ({@code ObjectContents}/{@code AtlasSprite}) this game version added. Moved here from
 * SkyMelloo's original AccountLinkedMarkerManager, generalized to take the sprite as a
 * parameter, and driven through this mod's own mixins only (nametag: {@code EntityDisplayNameMixin};
 * tab list: {@code PlayerTabOverlayMixin}) so only one marker ever gets added per player per
 * surface - never two mixins each independently prepending their own icon, which would stack
 * rather than override.
 * <p>
 * Keyed by {@link UUID}, not a loaded {@code Player} entity - the tab list can show players who
 * aren't even in render distance (see {@code PlayerTabOverlayMixin}'s own doc comment), so there's
 * no guaranteed {@code Player} object to key off for that surface, only a {@link UUID} from the
 * tab-list entry's game profile.
 * <p>
 * The pink-vs-light-blue choice itself is NOT decided here (previously a SkyMelloo-registered
 * override resolver lived in this class - removed, see git history if needed): both mods report
 * presence to the exact same {@code /presence} endpoint, so a UUID being "a mod user" was already
 * true for anyone running EITHER mod - the override could only ever see the LOCAL client's own
 * SkyMelloo presence data, not the server's, so a MellooEssentials-only player always looked pink
 * from a SkyMelloo client, and a real SkyMelloo user always looked light-blue from an Essentials-only
 * client. Callers now pass the already-decided sprite directly (see
 * {@link PresenceManager#isSkyMelloo}, server-resolved per UUID from which mod's client header showed
 * up on that UUID's most recent presence report - correct symmetrically from either mod's client).
 * No forced text color on either the sprite or its connecting space - both use an explicit but
 * colorless style ({@link Style#EMPTY}) purely to block an earlier rank/name color code from bleeding
 * onto the dye's own natural texture color.
 */
public final class ModMarkerManager {
	private static final String FALLBACK_GLYPH = "❖"; // used only if the sprite itself can't resolve

	private ModMarkerManager() {
	}

	public static Component apply(UUID uuid, Component original, Identifier spriteId) {
		MutableComponent fallback = Component.literal(FALLBACK_GLYPH).setStyle(Style.EMPTY);
		MutableComponent icon = MutableComponent.create(new ObjectContents(new AtlasSprite(AtlasIds.ITEMS, spriteId), Optional.of(fallback)));
		icon.setStyle(Style.EMPTY);
		// A thin space (U+2009), not a regular one - narrower gap than the original design, but the
		// name sitting flush against the icon with no gap at all read as a little too tight.
		MutableComponent spacer = Component.literal(" ").setStyle(Style.EMPTY);
		return icon.append(spacer).append(original);
	}

	/** Whether {@code uuid} should get a marker at all - true for the local player, or anyone {@link PresenceManager} has detected reporting presence. */
	public static boolean isModUser(UUID uuid) {
		var local = Minecraft.getInstance().player;
		return (local != null && local.getUUID().equals(uuid)) || PresenceManager.isModUser(uuid);
	}
}
