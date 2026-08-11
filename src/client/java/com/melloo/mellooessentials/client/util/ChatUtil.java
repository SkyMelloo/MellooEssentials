package com.melloo.mellooessentials.client.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/** Consistent chat message formatting for all MellooEssentials output - mirrors SkyMelloo's own ChatUtil, light-blue brand gradient instead of SkyMelloo's pink (matching the mod-user marker/website's own light-blue vs. pink distinction between the two mods). */
public final class ChatUtil {
	private static final int GRADIENT_START = 0x66DDFF;
	private static final int GRADIENT_END = 0x3FA9D9;

	private ChatUtil() {
	}

	public static MutableComponent prefixed(String message) {
		return prefixed(Component.literal(message));
	}

	/** Like {@link #prefixed(String)}, but for a message that needs rich formatting (click/hover events, mixed colors) rather than a plain string. */
	public static MutableComponent prefixed(Component message) {
		MutableComponent result = Component.literal("§b[").append(gradientText("MellooEssentials")).append(Component.literal("§b]§r "));
		result.append(message);
		return result;
	}

	private static MutableComponent gradientText(String text) {
		MutableComponent result = Component.empty();
		int len = text.length();
		for (int i = 0; i < len; i++) {
			float t = len <= 1 ? 0F : (float) i / (len - 1);
			int rgb = lerpColor(GRADIENT_START, GRADIENT_END, t);
			result.append(Component.literal(String.valueOf(text.charAt(i))).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))));
		}
		return result;
	}

	/**
	 * Public entry point for call sites that build their own chat line but still need
	 * CompletionException-unwrapping - without it, a failed command chain shows
	 * "java.lang.RuntimeException: <message>" instead of just "<message>", since
	 * CompletionException(cause)'s own getMessage() is cause.toString().
	 */
	public static String friendlyError(Throwable error) {
		Throwable cause = error;
		while (cause.getCause() != null && cause.getCause() != cause) {
			cause = cause.getCause();
		}
		if (cause instanceof java.net.http.HttpTimeoutException) {
			return Lang.s("mellooessentials.chat.error.timeout");
		}
		String msg = cause.getMessage();
		return msg != null && !msg.isBlank() ? msg : cause.getClass().getSimpleName();
	}

	private static int lerpColor(int from, int to, float t) {
		int r1 = (from >> 16) & 0xFF, g1 = (from >> 8) & 0xFF, b1 = from & 0xFF;
		int r2 = (to >> 16) & 0xFF, g2 = (to >> 8) & 0xFF, b2 = to & 0xFF;
		int r = Math.round(r1 + (r2 - r1) * t);
		int g = Math.round(g1 + (g2 - g1) * t);
		int b = Math.round(b1 + (b2 - b1) * t);
		return (r << 16) | (g << 8) | b;
	}
}
