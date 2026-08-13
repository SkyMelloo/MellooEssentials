package com.melloo.mellooessentials.client.util;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Central choke point for every {@code /pc} (party chat) send in the mod - lets a Hypixel rate-limit
 * rejection ("Woah slow down, you're doing that too fast!") retry the message that actually got
 * dropped instead of it just silently never reaching the party. Ported from SkyMelloo's identical
 * class alongside PartyGamesManager - every {@code /pc} call site should go through {@link #send}
 * rather than calling {@code sendCommand("pc ...")} directly.
 * <p>
 * Only remembers the ONE most recently sent command - Hypixel's rejection line doesn't say which
 * message it was rejecting, so this assumes it's whichever one was sent last. A retry window (rather
 * than remembering forever) keeps an unrelated, much-later rate limit from retrying a stale message.
 */
public final class PartyChatSender {
	private static final Logger LOGGER = LoggerFactory.getLogger("MellooEssentials/PartyChatSender");
	private static final Pattern RATE_LIMITED = Pattern.compile("(?i)woah slow down, you're doing that too fast!");
	private static final int RETRY_DELAY_TICKS = 100; // ~5s - clear of whatever window triggered the limit
	private static final long RETRY_ELIGIBLE_WINDOW_MILLIS = 3000;
	private static final int MAX_RETRIES = 1;

	private static boolean initialized = false;
	private static String lastSentCommand = null;
	private static long lastSentMillis = 0;
	private static int lastSentRetries = 0;

	private PartyChatSender() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (lastSentCommand == null || !RATE_LIMITED.matcher(message.getString()).find()) {
				return;
			}
			long now = System.currentTimeMillis();
			if (now - lastSentMillis > RETRY_ELIGIBLE_WINDOW_MILLIS || lastSentRetries >= MAX_RETRIES) {
				return;
			}
			lastSentRetries++;
			String toRetry = lastSentCommand;
			LOGGER.debug("Party chat rate-limited - retrying in {}s: {}", RETRY_DELAY_TICKS / 20, toRetry);
			TickDelay.schedule(RETRY_DELAY_TICKS, () -> {
				Minecraft client = Minecraft.getInstance();
				if (client.player != null && client.player.connection != null) {
					client.player.connection.sendCommand(toRetry);
				}
			});
		});
	}

	/** Sends {@code /pc <rawText>} (already-formatted, no further prefixing done here) and remembers it so a rate-limit rejection right after can retry it. */
	public static void send(Minecraft client, String rawText) {
		if (client.player == null || client.player.connection == null) {
			return;
		}
		String command = "pc " + rawText;
		lastSentCommand = command;
		lastSentMillis = System.currentTimeMillis();
		lastSentRetries = 0;
		client.player.connection.sendCommand(command);
	}
}
