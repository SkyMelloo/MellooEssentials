package com.melloo.mellooessentials.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.server.network.EventLoopGroupHolder;

// Real ping via Minecraft's own server-list-ping, independent of Hypixel's proxy-hop keep-alive
// number. pingServer has no failure callback at all, hence the safety timeout below.
public final class ServerPingMonitor {
	private static final int PING_INTERVAL_TICKS = 20; // 1s at 20 ticks/s
	private static final int PING_TIMEOUT_TICKS = 60; // 3s safety timeout

	private static final ServerStatusPinger PINGER = new ServerStatusPinger();
	private static volatile Long lastPingMillis = null;
	private static int tickCounter = 0;
	private static boolean pingInFlight = false;
	private static int inFlightTicks = 0;
	// 10 second window for the average/worst-case readout.
	private static final RollingStats HISTORY = new RollingStats(10);

	private ServerPingMonitor() {
	}

	public static void tick(Minecraft client) {
		PINGER.tick();
		if (pingInFlight) {
			inFlightTicks++;
			if (inFlightTicks > PING_TIMEOUT_TICKS) {
				pingInFlight = false; // neither callback ever fired - give up, let the next cycle retry
			}
			return;
		}
		ServerData current = client.getCurrentServer();
		if (current == null || current.ip == null || current.ip.isBlank()) {
			return;
		}
		tickCounter++;
		if (tickCounter < PING_INTERVAL_TICKS) {
			return;
		}
		tickCounter = 0;
		pingOnce(current.ip);
	}

	private static void pingOnce(String ip) {
		ServerData scratch = new ServerData("MellooEssentials Ping", ip, ServerData.Type.OTHER);
		pingInFlight = true;
		inFlightTicks = 0;
		try {
			PINGER.pingServer(scratch,
					() -> {
						// Status response - MOTD/player-count/version, not the ping measurement itself.
					},
					() -> {
						// Pong response - THIS is when ping is actually computed and set.
						lastPingMillis = scratch.ping;
						HISTORY.addSample(scratch.ping);
						pingInFlight = false;
					},
					EventLoopGroupHolder.remote(false));
		} catch (Exception e) {
			pingInFlight = false;
		}
	}

	public static Long getPingMillis() {
		return lastPingMillis;
	}

	public static Double getAveragePingMillis() {
		return HISTORY.hasSamples() ? HISTORY.average() : null;
	}

	// Average of the worst (highest) 1% of samples over the last 10 seconds.
	public static Double getWorstPingMillis() {
		return HISTORY.hasSamples() ? HISTORY.worstAverage(false) : null;
	}
}
