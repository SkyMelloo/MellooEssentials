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
	private static volatile String lastServerTypeName = null;

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
			// A hub/lobby has no mode/map at all (those are per-game concepts) - this is what's
			// actually populated there instead, e.g. LobbyType.MAIN's name is "Main Lobby".
			String serverTypeName = packet.getServerType().map(net.hypixel.data.type.ServerType::getName).orElse(null);
			if (Objects.equals(mode, lastMode) && Objects.equals(map, lastMap) && Objects.equals(serverTypeName, lastServerTypeName)) {
				return;
			}
			lastMode = mode;
			lastMap = map;
			lastServerTypeName = serverTypeName;
			LOGGER.debug("Hypixel location changed - server=" + packet.getServerName() + ", type=" + serverTypeName + ", mode=" + mode + ", map=" + map);
		});
	}

	public static String getMode() {
		return lastMode;
	}

	public static String getMap() {
		return lastMap;
	}

	/** Readable server-type name (e.g. "Main Lobby", "Bed Wars") - populated even when {@link #getMap()} isn't, since a hub has no map/mode concept at all. */
	public static String getServerTypeName() {
		return lastServerTypeName;
	}

	/** Best-effort only - see the class doc comment. Confirm the real mode/map values from the debug log before relying on this for anything that actually gates behavior. */
	public static boolean isLikelyInDungeon() {
		return containsDungeon(lastMode) || containsDungeon(lastMap);
	}

	private static boolean containsDungeon(String value) {
		return value != null && value.toLowerCase(Locale.ROOT).contains("dungeon");
	}
}
