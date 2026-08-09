package com.melloo.mellooessentials.client.social;

import com.melloo.mellooessentials.client.api.ModAuthManager;
import com.melloo.mellooessentials.client.config.EssentialsConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Persistent HUD showing whether this mod has authenticated with sky.melloo.me (the same
 * joinServer/hasJoined identity handshake {@link ModAuthManager} performs for presence/cosmetics
 * sync) and, once connected, how long it's been connected for - a live-ticking duration, not just a
 * static "Connected" label. Mirrors SkyMelloo's own WhitelistStatusHud in spirit, but this mod has
 * no whitelist concept at all - purely a build-authenticity/presence-sync signal.
 */
public final class ConnectionStatusHud implements HudElement {
	public static final ConnectionStatusHud INSTANCE = new ConnectionStatusHud();

	private ConnectionStatusHud() {
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

		String statusText;
		String subText = null;
		int statusColor;
		switch (ModAuthManager.getConnectionState()) {
			case CONNECTED -> {
				statusText = "Connected to sky.melloo.me";
				subText = "for " + formatDuration(System.currentTimeMillis() - ModAuthManager.getConnectedSince());
				statusColor = 0xFF55FF55;
			}
			case ERROR -> {
				statusText = "Connection failed - see sky.melloo.me/status";
				statusColor = 0xFFFF8800;
			}
			default -> {
				statusText = "Connecting to sky.melloo.me...";
				statusColor = 0xFFFFCC00;
			}
		}

		int x = config.hudConnectionStatusX >= 0 ? config.hudConnectionStatusX : 6;
		int y = config.hudConnectionStatusY >= 0 ? config.hudConnectionStatusY : 6;
		int width = Math.max(client.font.width(statusText), subText != null ? client.font.width(subText) : 0) + 18;
		int height = subText != null ? 23 : 11;

		gg.fill(x - 4, y - 3, x + width, y + height, 0x99101018);
		gg.fill(x, y + 1, x + 6, y + 7, statusColor);
		gg.text(client.font, statusText, x + 10, y, statusColor);
		if (subText != null) {
			gg.text(client.font, subText, x + 10, y + 12, 0xFFAAAAAA);
		}
	}
}
