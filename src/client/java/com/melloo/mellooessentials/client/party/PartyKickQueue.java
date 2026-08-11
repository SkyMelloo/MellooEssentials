package com.melloo.mellooessentials.client.party;

import com.melloo.mellooessentials.client.util.ChatUtil;
import com.melloo.mellooessentials.client.util.Lang;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

/**
 * Queued {@code /party kick} sending + the plain join-time kick/block chat prompt - moved here from
 * SkyMelloo's own PartyJoinWatcher, since none of it was ever actually SkyMelloo-specific: kicking is
 * a plain Hypixel command anyone can send, and blocking (see {@link BlockedUsersManager}) is now this
 * mod's own primitive too. Any mod (SkyMelloo's own threshold-based auto-kick rules, or a third
 * party) can call {@link #queueKick} directly instead of building a second queue/cooldown-retry
 * mechanism of its own.
 * <p>
 * Kicks are queued rather than sent immediately - Hypixel enforces its own ~1s per-command cooldown,
 * so several joins/checks landing close together used to silently fail every kick after the first
 * with "Command Failed: This command is on cooldown!". Drained one at a time, spaced out, with a
 * cooldown-failure retry (requeued at the front, not the back).
 */
public final class PartyKickQueue {
	private static final Deque<String> pendingKicks = new ArrayDeque<>();
	// The username of the kick command most recently actually sent, waiting to find out (via the
	// cooldown-failure message below) whether it actually landed.
	private static String pendingConfirmUsername = null;
	private static int tickCounter = 0;
	private static int nextKickAllowedTick = 0;
	// Hypixel's own message says "about a second" - 22 ticks (~1.1s) leaves a small safety margin
	// rather than cutting it exactly at 20.
	private static final int KICK_INTERVAL_TICKS = 22;
	private static final Pattern KICK_COOLDOWN_FAILED = Pattern.compile("Command Failed:.*cooldown", Pattern.CASE_INSENSITIVE);

	// Optional extra text/buttons another mod wants appended to the join-notification chat line -
	// left null/unregistered means nothing extra, same shape as every other optional hook here.
	private static volatile BiFunction<String, MutableComponent, MutableComponent> extraJoinAction = null;

	private static boolean initialized = false;

	private PartyKickQueue() {
	}

	public static void setExtraJoinAction(BiFunction<String, MutableComponent, MutableComponent> extra) {
		extraJoinAction = extra;
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (pendingConfirmUsername == null) {
				return;
			}
			if (KICK_COOLDOWN_FAILED.matcher(message.getString()).find()) {
				// The kick we just sent didn't actually go through - Hypixel's own per-command cooldown
				// rejected it. Requeue at the FRONT (not the back) so it's still the very next one
				// retried, and wait a full fresh interval again rather than assuming the cooldown clears
				// any sooner.
				pendingKicks.addFirst(pendingConfirmUsername);
				pendingConfirmUsername = null;
				nextKickAllowedTick = tickCounter + KICK_INTERVAL_TICKS;
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(PartyKickQueue::tick);
	}

	private static void tick(Minecraft client) {
		tickCounter++;
		if (!pendingKicks.isEmpty() && tickCounter >= nextKickAllowedTick && client.player != null) {
			String username = pendingKicks.pollFirst();
			pendingConfirmUsername = username;
			nextKickAllowedTick = tickCounter + KICK_INTERVAL_TICKS;
			client.player.connection.sendCommand("party kick " + username);
		}
	}

	/**
	 * Queues a {@code /party kick} instead of sending it immediately - see the class doc comment.
	 * Deduped against both the pending queue and whatever kick is currently awaiting confirmation, so
	 * the same username is never queued twice even if more than one independent check flags them.
	 */
	public static void queueKick(String username) {
		if (username.equalsIgnoreCase(pendingConfirmUsername)) {
			return;
		}
		for (String queued : pendingKicks) {
			if (queued.equalsIgnoreCase(username)) {
				return;
			}
		}
		pendingKicks.addLast(username);
	}

	/**
	 * Called once per genuinely new party member (any join path - invite, finder, etc.) once their
	 * username is known - the caller (currently SkyMelloo's own PartyHudManager, which already does
	 * the UUID-&gt;username resolution with tab-list/Mojang-API fallback) decides WHEN a join counts
	 * as new; this only decides WHAT to do about it. A blocked user is auto-kicked here instead of
	 * ever showing the buttons, so blocking someone really does mean "never let them back into my
	 * party" without needing to click anything again.
	 */
	public static void handleMemberJoined(Minecraft client, String username) {
		if (BlockedUsersManager.isBlocked(username)) {
			if (PartyTracker.isLocalPlayerLeader()) {
				queueKick(username);
				client.player.sendSystemMessage(ChatUtil.prefixed(Lang.c("mellooessentials.chat.party.auto_kicked", username)));
			}
			return;
		}
		MutableComponent line = Lang.c("mellooessentials.chat.party.joined", username);
		if (PartyTracker.isLocalPlayerLeader()) {
			line = line.append(Lang.c("mellooessentials.gui.party.kick_button").withStyle(style -> style
					.withColor(ChatFormatting.RED)
					.withBold(true)
					.withClickEvent(new ClickEvent.RunCommand("/party kick " + username))
					.withHoverEvent(new HoverEvent.ShowText(Lang.c("mellooessentials.tooltip.party.kick", username)))));
		}
		line = line.append(Lang.c("mellooessentials.gui.party.block_button").withStyle(style -> style
				.withColor(ChatFormatting.DARK_RED)
				.withBold(true)
				.withClickEvent(new ClickEvent.RunCommand("/me block " + username))
				.withHoverEvent(new HoverEvent.ShowText(Lang.c("mellooessentials.tooltip.party.block", username)))));
		BiFunction<String, MutableComponent, MutableComponent> extra = extraJoinAction;
		if (extra != null) {
			line = extra.apply(username, line);
		}
		client.player.sendSystemMessage(ChatUtil.prefixed(line));
	}
}
