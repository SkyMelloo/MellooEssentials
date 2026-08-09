package com.melloo.mellooessentials.client.mixin;

import com.melloo.mellooessentials.client.highlight.HighlightManager;
import com.melloo.mellooessentials.client.social.ModMarkerManager;
import com.melloo.mellooessentials.client.social.PresenceManager;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forces a party/team member's Tab-list (player list) row name to the fixed highlight color,
 * overriding whatever color Hypixel's own scoreboard-team rank formatting would otherwise show -
 * see {@link HighlightManager#colorizeTabListName} - and prepends the same mod-user marker
 * {@link EntityDisplayNameMixin} adds to the in-world nametag, so it shows up in the tab list too.
 * Keyed by {@link PlayerInfo}, not a loaded entity, since the Tab-list can show players who aren't
 * even in render distance.
 */
@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
	private static final Identifier LIGHT_BLUE_DYE_SPRITE = Identifier.withDefaultNamespace("item/light_blue_dye");
	private static final Logger LOGGER = LoggerFactory.getLogger("MellooEssentials/PlayerTabOverlayMixin");

	// Diagnostic only, added to chase a live report that the marker doesn't show in the tab list on
	// some clients (Lunar Client specifically) despite this mixin's own logic reading correctly - see
	// if it even fires there at all, and whether presence data exists for the uuid in question, rather
	// than guessing at a fix blind. Throttled per-uuid (not per-frame - this injection runs on every
	// tab-list row render) since a real tab list can hold 80+ entries.
	private static final long LOG_THROTTLE_MS = 5000;
	private static final Map<UUID, Long> lastLoggedAt = new ConcurrentHashMap<>();

	@Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
	private void mellooessentials$colorizeTabName(PlayerInfo playerInfo, CallbackInfoReturnable<Component> cir) {
		UUID uuid = playerInfo.getProfile().id();
		Component colorized = HighlightManager.colorizeTabListName(uuid, cir.getReturnValue());
		boolean isModUser = ModMarkerManager.isModUser(uuid);
		if (isModUser) {
			colorized = ModMarkerManager.apply(uuid, colorized, LIGHT_BLUE_DYE_SPRITE);
		}
		maybeLog(uuid, isModUser);
		cir.setReturnValue(colorized);
	}

	private static void maybeLog(UUID uuid, boolean markerApplied) {
		long now = System.currentTimeMillis();
		Long last = lastLoggedAt.get(uuid);
		if (last != null && now - last < LOG_THROTTLE_MS) {
			return;
		}
		lastLoggedAt.put(uuid, now);
		LOGGER.info("tab-list mixin fired for {} - isModUser={}, isStaff={}, markerApplied={}",
				uuid, PresenceManager.isModUser(uuid), PresenceManager.isStaff(uuid), markerApplied);
	}
}
