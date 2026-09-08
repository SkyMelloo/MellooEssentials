package com.melloo.mellooessentials.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.util.Locale;

// Whether the local player is connected to Hypixel, checked via the connected server's IP.
public final class HypixelDetector {
	private HypixelDetector() {
	}

	public static boolean isHypixel(Minecraft client) {
		ServerData server = client.getCurrentServer();
		return server != null && server.ip != null && server.ip.toLowerCase(Locale.ROOT).contains("hypixel");
	}
}
