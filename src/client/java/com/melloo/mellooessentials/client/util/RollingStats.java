package com.melloo.mellooessentials.client.util;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

// Rolling window (by wall-clock time) of numeric samples with average and "1% lows"/"1% highs".
// Every method is synchronized: written from a mixin thread, read back on the render thread.
public final class RollingStats {
	private record Sample(long timestampMillis, double value) {
	}

	private final long windowMillis;
	private final Deque<Sample> samples = new ArrayDeque<>();

	public RollingStats(int windowSeconds) {
		this.windowMillis = windowSeconds * 1000L;
	}

	public synchronized void addSample(double value) {
		long now = System.currentTimeMillis();
		samples.addLast(new Sample(now, value));
		long cutoff = now - windowMillis;
		while (!samples.isEmpty() && samples.peekFirst().timestampMillis() < cutoff) {
			samples.pollFirst();
		}
	}

	public synchronized boolean hasSamples() {
		return !samples.isEmpty();
	}

	public synchronized double average() {
		if (samples.isEmpty()) {
			return 0;
		}
		double sum = 0;
		for (Sample s : samples) {
			sum += s.value();
		}
		return sum / samples.size();
	}

	// lowest: true for "1% lows" (FPS/TPS), false for the worst-case where higher is worse (ping).
	public synchronized double worstAverage(boolean lowest) {
		if (samples.isEmpty()) {
			return 0;
		}
		List<Double> sorted = samples.stream()
				.map(Sample::value)
				.sorted(lowest ? Comparator.naturalOrder() : Comparator.reverseOrder())
				.toList();
		int count = Math.max(1, (int) Math.ceil(sorted.size() * 0.01));
		double sum = 0;
		for (int i = 0; i < count; i++) {
			sum += sorted.get(i);
		}
		return sum / count;
	}
}
