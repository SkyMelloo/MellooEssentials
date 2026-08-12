package com.melloo.mellooessentials.client.social;

import com.melloo.mellooessentials.client.api.ModAuthManager;
import com.melloo.mellooessentials.client.config.EssentialsConfig;
import com.melloo.mellooessentials.client.util.Lang;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.Supplier;

/**
 * Persistent HUD showing whether this mod has authenticated with sky.melloo.me (the same
 * joinServer/hasJoined identity handshake {@link ModAuthManager} performs for presence/cosmetics
 * sync) and, once connected, how long it's been connected for - a live-ticking duration, not just a
 * static "Connected" label. This is now the SINGLE connection-status HUD for both mods - SkyMelloo's
 * own WhitelistStatusHud (which used to duplicate this box, and whose separate ModAuthManager
 * session used to silently steal this one's server-side session slot - see lib/modAuth.js's fix)
 * was removed in favor of registering into the two extension points below instead, so SkyMelloo's
 * admin-link badge and its own sky.melloo.me ping reading still show up here, one box, not two.
 */
public final class ConnectionStatusHud implements HudElement {
	public static final ConnectionStatusHud INSTANCE = new ConnectionStatusHud();

	// Populated by SkyMelloo (when installed) - see SkyMellooClient#onInitializeClient. Left null
	// when only this mod is installed, since neither concept (an admin-linked account, a
	// sky.melloo.me API ping reading) exists here on its own.
	private static volatile Supplier<String> adminBadgeSupplier = null;
	private static volatile Supplier<String> extraLineProvider = null;

	private ConnectionStatusHud() {
	}

	/** The account's actual highest role label (e.g. "Owner") to show as "Connected as {role}" - null for a non-admin account, a SkyMelloo-only concept. */
	public static void setAdminBadgeSupplier(Supplier<String> supplier) {
		adminBadgeSupplier = supplier;
	}

	/** A short extra fragment appended to the detail line (e.g. SkyMelloo's own "42ms" sky.melloo.me API ping reading, NOT prefixed with "sky.melloo.me" itself - this HUD already names the domain once) - return null to omit it that frame. */
	public static void setExtraLineProvider(Supplier<String> provider) {
		extraLineProvider = provider;
	}

	/** A real filled circle via horizontal scanlines, not a flat square - {@code gg.fill} only draws rectangles, so this is the cheapest way to get a round dot at these tiny (2-6px) radii. */
	private static void fillCircle(GuiGraphicsExtractor gg, int cx, int cy, int radius, int color) {
		for (int dy = -radius; dy <= radius; dy++) {
			int dx = (int) Math.round(Math.sqrt((double) radius * radius - (double) dy * dy));
			gg.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
		}
	}

	/** "1h 05m 30s", dropping leading zero units - matches the compact style used elsewhere for durations. */
	private static String formatDuration(long millis) {
		long totalSeconds = millis / 1000;
		long hours = totalSeconds / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;
		if (hours > 0) {
			return String.format("%dh %02dm %02ds", hours, minutes, seconds);
		}
		if (minutes > 0) {
			return String.format("%dm %02ds", minutes, seconds);
		}
		return seconds + "s";
	}

	/**
	 * Always exactly two lines - a bold headline ("Connected"/"Connecting…"/"Connection Failed") and
	 * one detail line combining everything else (domain, duration, admin badge, ping) that used to be
	 * spread across up to three separate stacked lines. A round, gently pulsing status dot (a soft
	 * translucent halo breathing behind a solid center, both real circles via fillCircle rather than
	 * flat squares) sits right before the headline text, plus a colored accent stripe down the panel's
	 * left edge - the stripe and the dot share one color, reading as a single deliberate accent.
	 */
	@Override
	public void extractRenderState(GuiGraphicsExtractor gg, DeltaTracker deltaTracker) {
		EssentialsConfig config = EssentialsConfig.get();
		if (!config.connectionStatusHudEnabled) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}

		Supplier<String> adminSupplier = adminBadgeSupplier;
		String adminRole = adminSupplier != null ? adminSupplier.get() : null;
		Supplier<String> extraProvider = extraLineProvider;
		String extra = extraProvider != null ? extraProvider.get() : null;

		String headline;
		String detail;
		int statusColor;
		switch (ModAuthManager.getConnectionState()) {
			case CONNECTED -> {
				headline = "§l" + (adminRole != null && !adminRole.isEmpty()
						? Lang.s("mellooessentials.hud.connection.connected_as", adminRole) + " §d★"
						: Lang.s("mellooessentials.hud.connection.connected"));
				String duration = formatDuration(System.currentTimeMillis() - ModAuthManager.getConnectedSince());
				detail = "sky.melloo.me · " + duration + (extra != null ? " · " + extra : "");
				statusColor = 0xFF55FF55;
			}
			case ERROR -> {
				headline = "§l" + Lang.s("mellooessentials.hud.connection.failed");
				detail = Lang.s("mellooessentials.hud.connection.see_status") + (extra != null ? " · " + extra : "");
				statusColor = 0xFFFF6644;
			}
			default -> {
				headline = "§l" + Lang.s("mellooessentials.hud.connection.connecting");
				detail = "sky.melloo.me" + (extra != null ? " · " + extra : "");
				statusColor = 0xFFFFCC00;
			}
		}

		int x = config.hudConnectionStatusX >= 0 ? config.hudConnectionStatusX : 6;
		int y = config.hudConnectionStatusY >= 0 ? config.hudConnectionStatusY : 6;
		// Text gutter widened from 10px to 20px (see dotCx/textX below) - the dot's halo was nearly
		// touching both the accent stripe and the text at its peak pulse size, a live report confirmed
		// visible overlap. +20 -> +30 keeps the panel's own right-hand padding proportional to that.
		// +30 -> +42: the role-badge headline ("Connected as <Role> ★") visibly stuck out past the
		// box's right edge, a real report - the trailing "★" glyph measures narrower than it actually
		// renders, so the fixed +30 slack wasn't enough once the headline got this much longer.
		int width = Math.max(client.font.width(headline), client.font.width(detail)) + 42;
		int height = 26;

		// Panel: the same (x-4, y-3) dark-glass-fill origin every other HUD in both mods uses (keeps
		// this element's drag-preview box in SkyMelloo's HUD layout editor aligned with what actually
		// renders), plus a colored accent stripe down the left edge.
		gg.fill(x - 4, y - 3, x + width - 4, y - 3 + height, 0x99101018);
		gg.fill(x - 4, y - 3, x - 2, y - 3 + height, statusColor);

		// Gentle 2s breathing cycle (sine, not a linear ramp - reads as "alive" rather than mechanical)
		// driving both the halo's and the core's radius, not just their opacity - a size pulse is what
		// actually reads as "pulsing" at a glance, an opacity-only fade doesn't. Bumped up from
		// 4-6/2-3 to 5-7/3-4 - larger radii approximate a circle more smoothly via fillCircle's
		// scanlines (a tiny 2-3px "circle" reads as blocky/square at a glance, a real live report).
		double pulse = (Math.sin((System.currentTimeMillis() % 2000) / 2000.0 * Math.PI * 2) + 1) / 2;
		int haloRadius = 5 + (int) Math.round(pulse * 2);
		int coreRadius = 3 + (int) Math.round(pulse);

		// Centered on the headline's own text row, with real clearance on both sides now (2px from the
		// accent stripe on the left, ~6px from the text on the right, even at the halo's largest pulse
		// size) instead of nearly touching either one.
		int dotCx = x + 7;
		int dotCy = y + 4;
		int textX = x + 20;
		fillCircle(gg, dotCx, dotCy, haloRadius, (statusColor & 0xFFFFFF) | 0x40000000);
		fillCircle(gg, dotCx, dotCy, coreRadius, statusColor);

		gg.text(client.font, headline, textX, y, statusColor);
		gg.text(client.font, detail, textX, y + 12, 0xFFAAAAAA);
	}
}
