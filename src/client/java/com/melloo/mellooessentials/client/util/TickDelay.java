package com.melloo.mellooessentials.client.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs a callback a fixed number of client ticks from now - used to stagger a sequence of actions
 * (e.g. one /pc announcement per party member) rather than firing them back-to-back, which risks
 * tripping Hypixel's own chat rate limit. Not a general-purpose scheduler: just a flat list checked
 * once per tick, fine for the handful of concurrent delays this mod ever actually needs. Ported
 * over from SkyMelloo's own identical class alongside PartyGamesManager/PartyChatSender.
 */
public final class TickDelay {
	private record Pending(int[] ticksRemaining, Runnable task) {
	}

	private static final List<Pending> pending = new ArrayList<>();

	private TickDelay() {
	}

	public static void schedule(int delayTicks, Runnable task) {
		pending.add(new Pending(new int[]{delayTicks}, task));
	}

	/**
	 * Call once per client tick. Iterates a snapshot rather than the live list directly - a task run
	 * from here can itself call {@link #schedule} again, which would throw
	 * ConcurrentModificationException mutating {@code pending} mid-iteration otherwise. A
	 * newly-scheduled entry added mid-tick this way just isn't in this tick's snapshot, picked up
	 * starting next tick instead - correct anyway, since it was scheduled partway through this one.
	 */
	public static void tick() {
		if (pending.isEmpty()) {
			return;
		}
		List<Pending> snapshot = new ArrayList<>(pending);
		List<Pending> completed = new ArrayList<>();
		for (Pending entry : snapshot) {
			if (--entry.ticksRemaining()[0] > 0) {
				continue;
			}
			completed.add(entry);
			entry.task().run();
		}
		pending.removeAll(completed);
	}
}
