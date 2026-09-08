package com.melloo.mellooessentials.client.gui;

import com.melloo.mellooessentials.client.util.RollingStats;

// Rough server TPS estimate: measures how fast gameTime advances vs. real time between consecutive
// SetTimePacket updates. Uses an EMA rather than a flat average so a real TPS change shows fast.
public final class TpsEstimator {
	private static final double EMA_ALPHA = 0.5;

	private static Double smoothedTps = null;
	private static long lastRealTime = -1;
	private static long lastGameTime = -1;
	// 10 second window for the average/1%-low readout.
	private static final RollingStats HISTORY = new RollingStats(10);

	private TpsEstimator() {
	}

	public static void onSetTimePacket(long gameTime) {
		long now = System.currentTimeMillis();
		if (lastRealTime < 0) {
			lastRealTime = now;
			lastGameTime = gameTime;
			return;
		}
		long gameDelta = gameTime - lastGameTime;
		long realDeltaMillis = now - lastRealTime;
		lastRealTime = now;
		lastGameTime = gameTime;
		// A huge gameTime jump (/time set, rejoining) isn't a real tick-rate signal - skip it.
		if (gameDelta <= 0 || gameDelta >= 200 || realDeltaMillis <= 0) {
			return;
		}
		double instantTps = Math.min(20.0, gameDelta / (realDeltaMillis / 1000.0));
		smoothedTps = smoothedTps == null ? instantTps : smoothedTps * (1 - EMA_ALPHA) + instantTps * EMA_ALPHA;
		HISTORY.addSample(instantTps);
	}

	public static Integer getEstimatedTps() {
		return smoothedTps == null ? null : (int) Math.round(smoothedTps);
	}

	public static Double getAverageTps() {
		return HISTORY.hasSamples() ? HISTORY.average() : null;
	}

	// Average of the worst (lowest) 1% of samples over the last 10 seconds.
	public static Double getOnePercentLowTps() {
		return HISTORY.hasSamples() ? HISTORY.worstAverage(true) : null;
	}
}
