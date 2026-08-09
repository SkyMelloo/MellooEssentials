package com.melloo.mellooessentials.client.social;

import com.melloo.mellooessentials.client.api.ModAuthManager;
import com.melloo.mellooessentials.client.config.EssentialsConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.BooleanSupplier;
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
	private static volatile BooleanSupplier adminBadgeSupplier = null;
	private static volatile Supplier<String> extraLineProvider = null;

	private ConnectionStatusHud() {
	}

	/** Whether the CONNECTED line should show an "(Admin)" suffix - true only for an account verified-linked to the sky.melloo.me admin/dev/owner team, a SkyMelloo-only concept. */
	public static void setAdminBadgeSupplier(BooleanSupplier supplier) {
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

		BooleanSupplier adminSupplier = adminBadgeSupplier;
		boolean isAdmin = adminSupplier != null && adminSupplier.getAsBoolean();
		Supplier<String> extraProvider = extraLineProvider;
		String extra = extraProvider != null ? extraProvider.get() : null;

		String headline;
		String detail;
		int statusColor;
		switch (ModAuthManager.getConnectionState()) {
			case CONNECTED -> {
				headline = "§lConnected" + (isAdmin ? " §d★" : "");
				String duration = formatDuration(System.currentTimeMillis() - ModAuthManager.getConnectedSince());
				detail = "sky.melloo.me · " + duration + (extra != null ? " · " + extra : "");
				statusColor = 0xFF55FF55;
			}
			case ERROR -> {
				headline = "§lConnection Failed";
				detail = "see sky.melloo.me/status" + (extra != null ? " · " + extra : "");
				statusColor = 0xFFFF6644;
			}
			default -> {
				headline = "§lConnecting…";
				detail = "sky.melloo.me" + (extra != null ? " · " + extra : "");
				statusColor = 0xFFFFCC00;
			}
		}

		int x = config.hudConnectionStatusX >= 0 ? config.hudConnectionStatusX : 6;
		int y = config.hudConnectionStatusY >= 0 ? config.hudConnectionStatusY : 6;
		int width = Math.max(client.font.width(headline), client.font.width(detail)) + 20;
		int height = 26;

		// Panel: the same (x-4, y-3) dark-glass-fill origin every other HUD in both mods uses (keeps
		// this element's drag-preview box in SkyMelloo's HUD layout editor aligned with what actually
		// renders), plus a colored accent stripe down the left edge.
		gg.fill(x - 4, y - 3, x + width - 4, y - 3 + height, 0x99101018);
		gg.fill(x - 4, y - 3, x - 2, y - 3 + height, statusColor);

		// Gentle 2s breathing cycle (sine, not a linear ramp - reads as "alive" rather than mechanical)
		// driving both the halo's and the core's radius, not just their opacity - a size pulse is what
		// actually reads as "pulsing" at a glance, an opacity-only fade doesn't.
		double pulse = (Math.sin((System.currentTimeMillis() % 2000) / 2000.0 * Math.PI * 2) + 1) / 2;
		int haloRadius = 4 + (int) Math.round(pulse * 2);
		int coreRadius = 2 + (int) Math.round(pulse);

		// Centered on the headline's own text row (not straddling both lines like before) and sitting
		// directly in front of "Connected" itself, in the same left gutter the accent stripe leaves free.
		int dotCx = x + 3;
		int dotCy = y + 4;
		fillCircle(gg, dotCx, dotCy, haloRadius, (statusColor & 0xFFFFFF) | 0x40000000);
		fillCircle(gg, dotCx, dotCy, coreRadius, statusColor);

		gg.text(client.font, headline, x + 10, y, statusColor);
		gg.text(client.font, detail, x + 10, y + 12, 0xFFAAAAAA);
	}
}
