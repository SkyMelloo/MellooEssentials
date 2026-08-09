package com.melloo.mellooessentials.client.social;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * The last 10 distinct usernames typed into any friend/chat command argument, most-recent-first -
 * so re-targeting someone you just interacted with (accepted a request from, messaged, sent a
 * request to) doesn't mean retyping their name, even once they've logged off and no longer appear
 * in {@code suggestOnlinePlayers}'s online-player list. Session-only (not persisted to disk) -
 * matches the simplicity level of everything else here, and a fresh list each launch is fine for
 * "who did I just talk to" recall.
 */
public final class RecentUsernames {
	private static final int MAX_SIZE = 10;
	private static final Deque<String> recent = new ArrayDeque<>(MAX_SIZE);

	private RecentUsernames() {
	}

	/** Moves {@code username} to the front (case-insensitive dedup against whatever's already in the list), trimming down to {@link #MAX_SIZE}. */
	public static synchronized void record(String username) {
		if (username == null || username.isBlank()) {
			return;
		}
		recent.removeIf(existing -> existing.equalsIgnoreCase(username));
		recent.addFirst(username);
		while (recent.size() > MAX_SIZE) {
			recent.removeLast();
		}
	}

	public static synchronized List<String> get() {
		// LinkedHashSet just to hand back a stable, order-preserving snapshot type - callers only
		// ever iterate/stream this, never mutate it.
		return List.copyOf(new LinkedHashSet<>(recent));
	}
}
