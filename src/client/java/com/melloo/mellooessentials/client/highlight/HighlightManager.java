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

// Decides which players get the forced-glow highlight (in-world nametag + Tab-list row). Staff/party
// colors are fixed; friend highlighting stays user-configurable. Priority: staff > party > friend.
// Classification is keyed by UUID, not a loaded Entity - the Tab-list can show a player out of render distance.
public final class HighlightManager {
	// Same accent blue used for every bordered popup panel across the SkyMelloo family.
	private static final int PARTY_COLOR = 0xFF66DDFF;
	private static final int STAFF_COLOR = 0xFFFF66CC; // pink
	// Vanilla's full-heart HUD sprite, off the GUI atlas instead of the item atlas.
	private static final Identifier HEART_SPRITE = Identifier.withDefaultNamespace("hud/heart/full");

	private enum Category {
		STAFF, PARTY, FRIEND, NONE
	}

	private HighlightManager() {
	}

	// Lets SkyMelloo substitute a different color for a party member's glow/marker right now - used
	// for the low-HP blink, a SkyMelloo-only feature this mod has no HP data for on its own.
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

	// username is only needed for the friend check - pass null to skip it (staff/party resolve from uuid alone).
	private static Category classify(UUID uuid, String username) {
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

	// Ignores blink overrides - callers apply those themselves since only party needs the uuid for it.
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

	// Friend highlighting keeps its own opt-in toggle - forcing it can hide cosmetic layers from other mods.
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

	// null if none of staff/party/friend apply.
	public static Integer getFixedColor(UUID uuid, String username) {
		Category category = classify(uuid, username);
		if (category == Category.NONE) {
			return null;
		}
		return applyBlinkOverride(uuid, category, rawColorFor(category));
	}

	// Appends a heart icon rather than overwriting the name's style, so Hypixel's own rank color survives.
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

	// Unlike colorizeName, forces the whole name to the fixed color, discarding Hypixel's rank coloring.
	public static Component colorizeTabListName(UUID uuid, String username, Component original) {
		Integer color = getFixedColor(uuid, username);
		if (color == null) {
			return original;
		}
		return Component.literal(original.getString()).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0xFFFFFF)));
	}
}
