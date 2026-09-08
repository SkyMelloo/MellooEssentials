package com.melloo.mellooessentials.client.social;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;

// Last 10 distinct usernames typed into a friend/chat command, most-recent-first, so re-targeting
// someone still works after they log off. Session-only, not persisted to disk.
public final class RecentUsernames {
	private static final int MAX_SIZE = 10;
	private static final Deque<String> recent = new ArrayDeque<>(MAX_SIZE);

	private RecentUsernames() {
	}

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
		return List.copyOf(new LinkedHashSet<>(recent));
	}
}
