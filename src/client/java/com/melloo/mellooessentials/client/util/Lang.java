package com.melloo.mellooessentials.client.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Shared helper for routing user-facing text through Minecraft's translation-key system. */
public final class Lang {
	private Lang() {
	}

	public static MutableComponent c(String key, Object... args) {
		return Component.translatable(key, args);
	}

	/** Resolved display text for call sites (HUD lines, widget labels) that render a raw String rather than a Component. */
	public static String s(String key, Object... args) {
		return Component.translatable(key, args).getString();
	}
}
