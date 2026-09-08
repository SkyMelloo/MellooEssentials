package com.melloo.mellooessentials.client.social;

import com.melloo.mellooessentials.client.api.ModAuthManager;
import com.melloo.mellooessentials.client.config.EssentialsConfig;
import com.melloo.mellooessentials.client.util.Lang;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.Supplier;

// Persistent HUD showing sky.melloo.me auth status and a live-ticking connected duration. Single
// shared box for both mods - SkyMelloo plugs its admin badge and ping reading into the extension points below.
public final class ConnectionStatusHud implements HudElement {
	public static final ConnectionStatusHud INSTANCE = new ConnectionStatusHud();

	// Populated by SkyMelloo when installed; left null when only this mod is installed.
	private static volatile Supplier<String> adminBadgeSupplier = null;
	private static volatile Supplier<String> extraLineProvider = null;

	private ConnectionStatusHud() {
	}

	// Shown as "Connected as {role}" - null for a non-admin account.
	public static void setAdminBadgeSupplier(Supplier<String> supplier) {
		adminBadgeSupplier = supplier;
	}

	// A short extra fragment appended to the detail line (e.g. "42ms") - return null to omit it that frame.
	public static void setExtraLineProvider(Supplier<String> provider) {
		extraLineProvider = provider;
	}

	// A real filled circle via horizontal scanlines - gg.fill only draws rectangles.
	private static void fillCircle(GuiGraphicsExtractor gg, int cx, int cy, int radius, int color) {
		for (int dy = -radius; dy <= radius; dy++) {
			int dx = (int) Math.round(Math.sqrt((double) radius * radius - (double) dy * dy));
			gg.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
		}
	}

	// "1h 05m 30s", dropping leading zero units.
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

	// Always exactly two lines: a bold headline and a detail line combining domain/duration/badge/ping.
	// A pulsing status dot (halo + solid center) and a matching accent stripe share one color.
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
		int width = Math.max(client.font.width(headline), client.font.width(detail)) + 42;
		int height = 26;

		// Same (x-4, y-3) dark-glass-fill origin every other HUD uses, plus a colored accent stripe on the left edge.
		gg.fill(x - 4, y - 3, x + width - 4, y - 3 + height, 0x99101018);
		gg.fill(x - 4, y - 3, x - 2, y - 3 + height, statusColor);

		// A size pulse (not just opacity) is what actually reads as "pulsing" at a glance.
		double pulse = (Math.sin((System.currentTimeMillis() % 2000) / 2000.0 * Math.PI * 2) + 1) / 2;
		int haloRadius = 5 + (int) Math.round(pulse * 2);
		int coreRadius = 3 + (int) Math.round(pulse);

		int dotCx = x + 7;
		int dotCy = y + 4;
		int textX = x + 20;
		fillCircle(gg, dotCx, dotCy, haloRadius, (statusColor & 0xFFFFFF) | 0x40000000);
		fillCircle(gg, dotCx, dotCy, coreRadius, statusColor);

		gg.text(client.font, headline, textX, y, statusColor);
		gg.text(client.font, detail, textX, y + 12, 0xFFAAAAAA);
	}
}
