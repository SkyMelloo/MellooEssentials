package com.melloo.mellooessentials.client.highlight;

import com.melloo.mellooessentials.client.party.PartyTracker;
import com.melloo.mellooessentials.client.social.PresenceManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Decides which players get the forced-glow highlight treatment (in-world nametag + Tab-list row).
 * Both colors are fixed, not user-adjustable - party is always light blue, sky.melloo.me team
 * members (any role at all - contributor/admin/moderator/etc, resolved server-side via
 * {@link PresenceManager}, never something a player could fake for themselves) are always pink.
 * Staff takes priority over party for someone who's both. Classification is keyed by UUID, not a
 * loaded {@link Entity} - the Tab-list can show a player who isn't even in render distance.
 */
public final class HighlightManager {
	// The same accent blue used for every bordered popup panel across both this mod (SettingsScreen,
	// CosmeticEditScreen, BulkCosmeticScreen) and sky.melloo.ch's own screens (StringInputScreen,
	// HudLayoutEditorScreen, etc.) - one consistent "light blue" across the whole SkyMelloo family,
	// not a separate shade invented just for this.
	private static final int PARTY_COLOR = 0xFF66DDFF;
	private static final int STAFF_COLOR = 0xFFFF66CC; // pink

	private enum Category {
		STAFF, PARTY, NONE
	}

	private HighlightManager() {
	}

	private static Category classify(UUID uuid) {
		// Staff checked first - always takes priority over party for someone who's both, not
		// user-configurable either way.
		if (PresenceManager.isStaff(uuid)) {
			return Category.STAFF;
		}
		if (PartyTracker.isMember(uuid)) {
			return Category.PARTY;
		}
		return Category.NONE;
	}

	private static Category classify(Entity entity) {
		return entity instanceof Player player ? classify(player.getUUID()) : Category.NONE;
	}

	public static boolean shouldGlow(Entity entity) {
		return classify(entity) != Category.NONE;
	}

	public static int getGlowColor(Entity entity) {
		return classify(entity) == Category.STAFF ? STAFF_COLOR : PARTY_COLOR;
	}

	/** The fixed ARGB color this UUID's Tab-list row/nametag should be forced to, or {@code null} if neither staff nor party. */
	public static Integer getFixedColor(UUID uuid) {
		return switch (classify(uuid)) {
			case STAFF -> STAFF_COLOR;
			case PARTY -> PARTY_COLOR;
			case NONE -> null;
		};
	}

	/**
	 * Appends a small colored marker after a highlighted player's in-world nametag, instead of
	 * overwriting the whole name's style - Hypixel bakes rank color (MVP+/VIP/etc.) into the name
	 * via the scoreboard team style, and flattening the whole component to one color would wipe
	 * that out. (The Tab-list version, {@link #colorizeTabListName}, deliberately does the opposite -
	 * see its own doc comment.)
	 */
	public static Component colorizeName(Player player, Component original) {
		Category category = classify(player.getUUID());
		if (category == Category.NONE) {
			return original;
		}
		TextColor color = TextColor.fromRgb((category == Category.STAFF ? STAFF_COLOR : PARTY_COLOR) & 0xFFFFFF);
		MutableComponent copy = original.copy();
		copy.append(Component.literal(" ♥").withStyle(Style.EMPTY.withColor(color)));
		return copy;
	}

	/**
	 * Forces the WHOLE Tab-list name to the fixed color, discarding Hypixel's own rank-color
	 * formatting entirely (re-literalized from the plain text) - unlike the in-world nametag marker
	 * above, this is meant to take priority over the server's own coloring, not just add to it.
	 */
	public static Component colorizeTabListName(UUID uuid, Component original) {
		Integer color = getFixedColor(uuid);
		if (color == null) {
			return original;
		}
		return Component.literal(original.getString()).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0xFFFFFF)));
	}
}
