package com.melloo.mellooessentials.client.gui;

import com.melloo.mellooessentials.client.config.EssentialsConfig;
import com.melloo.mellooessentials.client.social.HypixelLocationTracker;
import com.melloo.mellooessentials.client.util.Lang;
import com.melloo.mellooessentials.client.util.ServerPingMonitor;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ServerData;

import java.util.ArrayList;
import java.util.List;

// FPS, ping, TPS, server address, coordinates, area, and facing direction. FPS/TPS/ping show as
// "current average (1%low)", color-coded by the current reading, fixed-width padded.
public final class PlayerInfoHud implements HudElement {
	public static final PlayerInfoHud INSTANCE = new PlayerInfoHud();

	private static final String[] COMPASS_DIRECTION_KEYS = {
			"mellooessentials.hud.player_info.direction.s", "mellooessentials.hud.player_info.direction.sw",
			"mellooessentials.hud.player_info.direction.w", "mellooessentials.hud.player_info.direction.nw",
			"mellooessentials.hud.player_info.direction.n", "mellooessentials.hud.player_info.direction.ne",
			"mellooessentials.hud.player_info.direction.e", "mellooessentials.hud.player_info.direction.se"
	};

	private PlayerInfoHud() {
	}

	private static String compassDirection(float yaw) {
		float normalized = yaw % 360F;
		if (normalized < 0) {
			normalized += 360F;
		}
		int index = Math.round(normalized / 45F) % 8;
		return Lang.s(COMPASS_DIRECTION_KEYS[index]);
	}

	// higherIsBetter is true for FPS/TPS, false for ping.
	private static String colorFor(double current, double goodThreshold, double badThreshold, boolean higherIsBetter) {
		boolean good = higherIsBetter ? current >= goodThreshold : current <= goodThreshold;
		boolean bad = higherIsBetter ? current <= badThreshold : current >= badThreshold;
		if (good) {
			return "§a";
		}
		if (bad) {
			return "§c";
		}
		return "§6";
	}

	private static String formatStat(String label, int current, Double average, Double worst, int padWidth, double goodThreshold, double badThreshold, boolean higherIsBetter, String unit) {
		String color = colorFor(current, goodThreshold, badThreshold, higherIsBetter);
		String currentStr = String.format("%" + padWidth + "d", current);
		String avgStr = average != null ? String.format("%" + padWidth + "d", Math.round(average)) : "--";
		String worstStr = worst != null ? String.valueOf(Math.round(worst)) : "--";
		return "§r" + label + ": " + color + currentStr + unit + " §7" + avgStr + unit + " §8(" + worstStr + unit + ")";
	}

	// Shared by extractRenderState and HudLayoutEditorScreen's placeholder box, so the editor's
	// preview size can never drift from what actually renders.
	public static List<String> buildLines(Minecraft client) {
		List<String> lines = new ArrayList<>();
		lines.add(formatStat(Lang.s("mellooessentials.hud.player_info.fps"), client.getFps(), FpsMonitor.getAverageFps(), FpsMonitor.getOnePercentLowFps(), 3, 60, 30, true, ""));
		// Independently-measured ping, not Hypixel's own server-reported tab latency.
		Long realPing = ServerPingMonitor.getPingMillis();
		if (realPing != null) {
			Double avgPing = ServerPingMonitor.getAveragePingMillis();
			Double worstPing = ServerPingMonitor.getWorstPingMillis();
			lines.add(formatStat(Lang.s("mellooessentials.hud.player_info.ping"), realPing.intValue(), avgPing, worstPing, 3, 80, 200, false, "ms"));
		}
		Integer tps = TpsEstimator.getEstimatedTps();
		if (tps != null) {
			Double avgTps = TpsEstimator.getAverageTps();
			Double worstTps = TpsEstimator.getOnePercentLowTps();
			lines.add(formatStat(Lang.s("mellooessentials.hud.player_info.tps"), tps, avgTps, worstTps, 2, 18, 10, true, ""));
		}
		ServerData server = client.getCurrentServer();
		if (server != null && server.ip != null) {
			lines.add("§r" + Lang.s("mellooessentials.hud.player_info.server") + ": §f" + server.ip);
		}
		// Falls back to server-type name, then raw mode string, when there's no map.
		String area = HypixelLocationTracker.getMap();
		if (area == null) {
			area = HypixelLocationTracker.getServerTypeName();
		}
		if (area == null) {
			area = HypixelLocationTracker.getMode();
		}
		if (area != null) {
			lines.add("§r" + Lang.s("mellooessentials.hud.player_info.area") + ": §f" + area);
		}
		if (client.player != null) {
			lines.add("§r" + Lang.s("mellooessentials.hud.player_info.xyz") + ": §f" + (int) client.player.getX() + " " + (int) client.player.getY() + " " + (int) client.player.getZ());
			lines.add("§r" + Lang.s("mellooessentials.hud.player_info.facing") + ": §f" + compassDirection(client.player.getYRot()));
		}
		return lines;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor gg, DeltaTracker deltaTracker) {
		EssentialsConfig config = EssentialsConfig.get();
		if (!config.playerInfoHudEnabled) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}

		List<String> lines = buildLines(client);

		// Defaults to the top-left corner until moved via the HUD layout editor (key J).
		int x = config.hudPlayerInfoX >= 0 ? config.hudPlayerInfoX : 6;
		int y = config.hudPlayerInfoY >= 0 ? config.hudPlayerInfoY : 6;
		int width = 8;
		for (String line : lines) {
			width = Math.max(width, client.font.width(line) + 8);
		}
		int height = 4 + lines.size() * 10 + 2;

		gg.fill(x - 4, y - 3, x + width, y + height, 0x99101018);
		int lineY = y;
		for (String line : lines) {
			gg.text(client.font, line, x, lineY, 0xFFFFFFFF);
			lineY += 10;
		}
	}
}
