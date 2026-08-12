package com.melloo.mellooessentials.client.social;

import com.melloo.mellooessentials.client.api.ApiClient;
import com.melloo.mellooessentials.client.api.ModAuthManager;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Whether the current Minecraft account is linked to a sky.melloo.me website account - fetched
 * once after joining, then rechecked periodically (e.g. right after running "/me verify") so a
 * fresh link is picked up without reconnecting. Backs the accountLinked field in presence reports.
 */
public final class AccountLinkStatus {
	private static final int PERIODIC_RECHECK_TICKS = 600; // 30s at 20 ticks/s

	private static final Logger LOGGER = LoggerFactory.getLogger("MellooEssentials/AccountLinkStatus");
	private static volatile boolean linked = false;
	private static volatile boolean fetchInFlight = false;
	// Starts already past the threshold so the very first tick() after joining fetches immediately,
	// not after waiting a full recheck interval.
	private static int periodicTicks = PERIODIC_RECHECK_TICKS;

	private AccountLinkStatus() {
	}

	public static boolean isLinked() {
		return linked;
	}

	public static void tick(Minecraft client) {
		if (client.player == null) {
			return;
		}
		periodicTicks++;
		if (periodicTicks < PERIODIC_RECHECK_TICKS) {
			return;
		}
		periodicTicks = 0;
		refresh(client);
	}

	private static void refresh(Minecraft client) {
		if (fetchInFlight) {
			return;
		}
		fetchInFlight = true;
		ModAuthManager.getIdentity(client)
				.thenCompose(ApiClient::fetchPermissions)
				.whenComplete((result, error) -> {
					fetchInFlight = false;
					if (error != null || result == null) {
						LOGGER.debug("Account-link fetch failed" + (error != null ? " (" + error.getMessage() + ")" : "") + ".");
						return;
					}
					linked = Boolean.TRUE.equals(result.get("accountLinked"));
				});
	}
}
