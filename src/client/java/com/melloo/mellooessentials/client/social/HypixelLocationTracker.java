package com.melloo.mellooessentials.client.social;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;

import java.util.Locale;
import java.util.Objects;

/**
 * The local player's current world/area, per Hypixel's OWN official Mod API location event
 * (already a dependency - see PartyTracker, which uses the same library for party info) - used for
 * the Player Info HUD's "Area" line. Hypixel's exact mode/map string values aren't documented
 * anywhere public, so the raw fields are logged (debug level) for anyone who needs to double-check
 * a specific reading.
 */
public final class HypixelLocationTracker {
	private static final Logger LOGGER = LoggerFactory.getLogger("MellooEssentials/HypixelLocationTracker");
	private static boolean initialized = false;
	private static volatile String lastMode = null;
	private static volatile String lastMap = null;

	private HypixelLocationTracker() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		HypixelModAPI.getInstance().subscribeToEventPacket(ClientboundLocationPacket.class);
		HypixelModAPI.getInstance().createHandler(ClientboundLocationPacket.class, packet -> {
			String mode = packet.getMode().orElse(null);
			String map = packet.getMap().orElse(null);
			if (Objects.equals(mode, lastMode) && Objects.equals(map, lastMap)) {
				return;
			}
			lastMode = mode;
			lastMap = map;
			LOGGER.debug("Hypixel location changed - server=" + packet.getServerName() + ", mode=" + mode + ", map=" + map);
		});
	}

	public static String getMode() {
		return lastMode;
	}

	public static String getMap() {
		return lastMap;
	}

	/** Best-effort only - see the class doc comment. Confirm the real mode/map values from the debug log before relying on this for anything that actually gates behavior. */
	public static boolean isLikelyInDungeon() {
		return containsDungeon(lastMode) || containsDungeon(lastMap);
	}

	private static boolean containsDungeon(String value) {
		return value != null && value.toLowerCase(Locale.ROOT).contains("dungeon");
	}
}
