package com.melloo.mellooessentials.client.highlight;

import com.melloo.mellooessentials.client.config.EssentialsConfig;
import com.melloo.mellooessentials.client.party.PartyTracker;
import com.melloo.mellooessentials.client.social.FriendsManager;
import com.melloo.mellooessentials.client.social.PresenceManager;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * Decides which players get the forced-glow highlight treatment (in-world nametag + Tab-list row).
 * Staff and party colors are fixed, not user-adjustable - party is always light blue, sky.melloo.me
 * team members (any role at all - contributor/admin/moderator/etc, resolved server-side via
 * {@link PresenceManager}, never something a player could fake for themselves) are always pink.
 * Friend highlighting (see {@link EssentialsConfig#friendHighlightEnabled}) is the one category that
 * stays user-configurable - unlike staff/party, "which color represents MY friends to ME" is a
 * legitimate personal preference, not a shared fact. Priority when someone matches more than one
 * category: staff > party > friend. Classification is keyed by UUID, not a loaded {@link Entity} -
 * the Tab-list can show a player who isn't even in render distance.
 */
public final class HighlightManager {
	// The same accent blue used for every bordered popup panel across both this mod (SettingsScreen,
	// CosmeticEditScreen, BulkCosmeticScreen) and SkyMelloo's own screens (StringInputScreen,
	// HudLayoutEditorScreen, etc.) - one consistent "light blue" across the whole SkyMelloo family,
	// not a separate shade invented just for this.
	private static final int PARTY_COLOR = 0xFF66DDFF;
	private static final int STAFF_COLOR = 0xFFFF66CC; // pink
	// Vanilla's own full-heart HUD sprite (the exact icon the health bar itself uses) - a real heart,
	// not the tiny "♥" text glyph this used to be, which read as an odd little mark rather than an
	// actual heart at nametag text size. Same inline-sprite technique ModMarkerManager's dye icon uses
	// (ObjectContents/AtlasSprite), just off the GUI atlas instead of the item atlas.
	private static final Identifier HEART_SPRITE = Identifier.withDefaultNamespace("hud/heart/full");

	private enum Category {
		STAFF, PARTY, FRIEND, NONE
	}

	private HighlightManager() {
	}

	// Lets SkyMelloo (which depends on this mod, never the other way around) substitute a different
	// color for a specific party member's glow/marker right now - used for the low-HP blink (flashes
	// red under 25% HP), a SkyMelloo-only feature this mod has no data for on its own. Passed the
	// uuid and the color that would otherwise apply; returns a replacement or null to leave it as-is.
	// Safe in a way the old (removed) sprite-override hook wasn't: this always recomputes fresh from
	// local HP data on both sides, never a one-way "which mod is this" guess that could go stale/wrong
	// depending on which client is asking.
	private static volatile BiFunction<UUID, Integer, Integer> partyBlinkColorOverride = null;

	public static void setPartyBlinkColorOverride(BiFunction<UUID, Integer, Integer> resolver) {
		partyBlinkColorOverride = resolver;
	}

	private static int applyBlinkOverride(UUID uuid, Category category, int color) {
		if (category != Category.PARTY) {
			return color;
		}
		BiFunction<UUID, Integer, Integer> override = partyBlinkColorOverride;
		if (override == null) {
			return color;
		}
		Integer replaced = override.apply(uuid, color);
		return replaced != null ? replaced : color;
	}

	/** {@code username} is only needed for the friend check (case-insensitive name match, see FriendsManager) - pass null to skip it (staff/party still resolve fine from uuid alone). */
	private static Category classify(UUID uuid, String username) {
		// Staff checked first - always takes priority over party/friend for someone who's more than
		// one, not user-configurable either way.
		if (PresenceManager.isStaff(uuid)) {
			return Category.STAFF;
		}
		if (PartyTracker.isMember(uuid)) {
			return Category.PARTY;
		}
		if (username != null && EssentialsConfig.get().friendHighlightEnabled && FriendsManager.isFriend(username)) {
			return Category.FRIEND;
		}
		return Category.NONE;
	}

	private static Category classify(Entity entity) {
		return entity instanceof Player player ? classify(player.getUUID(), player.getName().getString()) : Category.NONE;
	}

	/** The color a given category renders as, ignoring blink overrides - callers apply those themselves since only some (party) need the uuid for it. */
	private static int rawColorFor(Category category) {
		return switch (category) {
			case STAFF -> STAFF_COLOR;
			case PARTY -> PARTY_COLOR;
			case FRIEND -> toRgb(EssentialsConfig.get().friendHighlightColor);
			case NONE -> PARTY_COLOR; // unreachable - every caller already guards on NONE first
		};
	}

	private static int toRgb(java.awt.Color color) {
		return color.getRGB() | 0xFF000000;
	}

	/**
	 * Staff/party always force the real glow outline (visible through walls) - that's the whole point
	 * for a shared team fact. Friend highlighting keeps its own opt-in toggle instead (see
	 * {@link EssentialsConfig#friendGlowOutlineEnabled}): forcing it by default can hide cosmetic
	 * layers from mods like Lunar Client for some players, and the colored nametag marker alone
	 * already gives a see-through indicator without that tradeoff.
	 */
	public static boolean shouldGlow(Entity entity) {
		Category category = classify(entity);
		if (category == Category.NONE) {
			return false;
		}
		if (category == Category.FRIEND) {
			return EssentialsConfig.get().friendGlowOutlineEnabled;
		}
		return true;
	}

	public static int getGlowColor(Entity entity) {
		Category category = classify(entity);
		int base = rawColorFor(category);
		UUID uuid = entity instanceof Player player ? player.getUUID() : null;
		return uuid != null ? applyBlinkOverride(uuid, category, base) : base;
	}

	/** The fixed ARGB color this UUID's Tab-list row/nametag should be forced to, or {@code null} if none of staff/party/friend apply. */
	public static Integer getFixedColor(UUID uuid, String username) {
		Category category = classify(uuid, username);
		if (category == Category.NONE) {
			return null;
		}
		return applyBlinkOverride(uuid, category, rawColorFor(category));
	}

	/**
	 * Appends a real heart icon after a highlighted player's in-world nametag, instead of overwriting
	 * the whole name's style - Hypixel bakes rank color (MVP+/VIP/etc.) into the name via the
	 * scoreboard team style, and flattening the whole component to one color would wipe that out.
	 * (The Tab-list version, {@link #colorizeTabListName}, deliberately does the opposite - see its
	 * own doc comment.)
	 */
	public static Component colorizeName(Player player, Component original) {
		UUID uuid = player.getUUID();
		Category category = classify(uuid, player.getName().getString());
		if (category == Category.NONE) {
			return original;
		}
		int rawColor = applyBlinkOverride(uuid, category, rawColorFor(category));
		TextColor color = TextColor.fromRgb(rawColor & 0xFFFFFF);
		MutableComponent fallback = Component.literal("♥").setStyle(Style.EMPTY);
		MutableComponent icon = MutableComponent.create(new ObjectContents(new AtlasSprite(AtlasIds.GUI, HEART_SPRITE), Optional.of(fallback)));
		icon.setStyle(Style.EMPTY.withColor(color));
		MutableComponent copy = original.copy();
		copy.append(Component.literal(" ").withStyle(Style.EMPTY)).append(icon);
		return copy;
	}

	/**
	 * Forces the WHOLE Tab-list name to the fixed color, discarding Hypixel's own rank-color
	 * formatting entirely (re-literalized from the plain text) - unlike the in-world nametag marker
	 * above, this is meant to take priority over the server's own coloring, not just add to it.
	 */
	public static Component colorizeTabListName(UUID uuid, String username, Component original) {
		Integer color = getFixedColor(uuid, username);
		if (color == null) {
			return original;
		}
		return Component.literal(original.getString()).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0xFFFFFF)));
	}
}
