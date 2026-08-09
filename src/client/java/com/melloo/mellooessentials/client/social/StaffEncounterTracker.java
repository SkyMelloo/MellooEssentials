package com.melloo.mellooessentials.client.social;

import com.melloo.mellooessentials.client.api.ApiClient;
import com.melloo.mellooessentials.client.api.ModAuthManager;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Scans the tab list for real staff/owner members and reports every sighting to sky.melloo.me,
 * which keeps its own per-account log of who you've ever been seen alongside (see
 * "/mellooessentials hitstaff"). Deliberately NOT gated to Hypixel/SkyBlock - unlike most other
 * features, this is meant to keep working on any server, since the whole point is recognizing a
 * staff member wherever you happen to run into them. Moved here from SkyMelloo (which had this
 * originally) since it's a general mod-identity feature, not a SkyBlock-specific one.
 * <p>
 * The server itself is the source of truth for who's actually staff (via roleForUuid, an
 * account's linked-Minecraft-account + team-role record) - this only ever sends candidate
 * uuid/username pairs from the tab list, never a role guess of its own.
 */
public final class StaffEncounterTracker {
	private static final int SCAN_INTERVAL_TICKS = 200; // 10s - tab list doesn't change fast enough to need tighter polling
	private static final Logger LOGGER = LoggerFactory.getLogger("MellooEssentials/StaffEncounterTracker");
	private static int tickCounter = 0;
	private static volatile boolean reportInFlight = false;

	private StaffEncounterTracker() {
	}

	public static void tick(Minecraft client) {
		if (client.getConnection() == null || client.player == null) {
			return;
		}
		tickCounter++;
		if (tickCounter % SCAN_INTERVAL_TICKS != 0) {
			return;
		}
		scanAndReport(client);
	}

	private static void scanAndReport(Minecraft client) {
		if (reportInFlight) {
			return;
		}
		UUID self = client.player.getUUID();
		List<ApiClient.StaffCheckEntry> players = new ArrayList<>();
		for (var info : client.getConnection().getOnlinePlayers()) {
			UUID id = info.getProfile().id();
			String name = info.getProfile().name();
			// Hypixel NPCs commonly show up in the tab list with names starting with "!" (e.g.
			// "!Auctioneer") - not real players, never worth a lookup.
			if (id.equals(self) || name.startsWith("!")) {
				continue;
			}
			players.add(new ApiClient.StaffCheckEntry(id.toString(), name));
		}
		if (players.isEmpty()) {
			return;
		}
		reportInFlight = true;
		ModAuthManager.getIdentity(client)
				.exceptionally(error -> null)
				.thenCompose(identity -> ApiClient.reportStaffEncounters(players, identity))
				.whenComplete((ignored, error) -> {
					reportInFlight = false;
					if (error != null) {
						LOGGER.debug("Staff-encounter scan failed ({}).", error.getMessage());
					} else {
						LOGGER.debug("Staff-encounter scan sent for {} nearby player(s).", players.size());
					}
				});
	}
}
