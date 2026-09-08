package com.melloo.mellooessentials.client.social;

import com.melloo.mellooessentials.client.api.ApiClient;
import com.melloo.mellooessentials.client.api.ModAuthManager;
import com.melloo.mellooessentials.client.party.PartyTracker;
import com.melloo.mellooessentials.client.util.ChatUtil;
import com.melloo.mellooessentials.client.util.Lang;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

// Short message relay riding entirely on sky.melloo.me - a "/mes chat" DM or party broadcast that
// never touches real Hypixel chat. Polls rather than pushes, tighter interval than PresenceManager.
public final class RelayChatManager {
	private static final int POLL_INTERVAL_TICKS = 60; // 3s
	private static final Logger LOGGER = LoggerFactory.getLogger("MellooEssentials/RelayChatManager");
	private static int tickCounter = 0;
	private static boolean pollInFlight = false;

	private RelayChatManager() {
	}

	public static void tick(Minecraft client) {
		if (client.player == null) {
			return;
		}
		tickCounter++;
		if (tickCounter % POLL_INTERVAL_TICKS == 0) {
			poll(client);
		}
	}

	private static void poll(Minecraft client) {
		if (pollInFlight) {
			return;
		}
		pollInFlight = true;
		ModAuthManager.getIdentity(client)
				.thenCompose(ApiClient::fetchRelayInbox)
				.whenComplete((messages, error) -> Minecraft.getInstance().execute(() -> {
					pollInFlight = false;
					if (error != null) {
						LOGGER.debug("Relay chat: inbox poll failed ({}).", error.getMessage());
						return;
					}
					for (ApiClient.RelayMessage message : messages) {
						display(message);
					}
				}));
	}

	private static void display(ApiClient.RelayMessage message) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		boolean isParty = "party".equalsIgnoreCase(message.scope());
		String tag = isParty ? "§d[Party] " : "§d[DM] ";
		MutableComponent name = Component.literal("§b" + message.fromUsername());
		if (!isParty) {
			// Click a DM sender's name to pre-fill a reply, same as clicking a name in vanilla chat.
			name.setStyle(Style.EMPTY
					.withClickEvent(new ClickEvent.SuggestCommand("/mes chat " + message.fromUsername() + " "))
					.withHoverEvent(new HoverEvent.ShowText(Lang.c("mellooessentials.tooltip.chat.reply"))));
		}
		client.player.sendSystemMessage(ChatUtil.prefixed(Component.literal(tag).append(name).append(Component.literal("§7: §f" + message.text()))));
	}

	public static void sendDirect(Minecraft client, String toUsername, String text) {
		if (!FriendsManager.isFriend(toUsername)) {
			client.player.sendSystemMessage(ChatUtil.prefixed(Lang.c("mellooessentials.chat.dm.not_friend", toUsername)));
			return;
		}
		RecentUsernames.record(toUsername);
		ModAuthManager.getIdentity(client)
				.thenCompose(identity -> ApiClient.sendRelayMessage(toUsername, text, identity))
				.whenComplete((ok, error) -> Minecraft.getInstance().execute(() -> {
					if (client.player == null) {
						return;
					}
					if (error != null || !Boolean.TRUE.equals(ok)) {
						client.player.sendSystemMessage(ChatUtil.prefixed(Lang.c("mellooessentials.chat.dm.send_failed")));
						return;
					}
					client.player.sendSystemMessage(ChatUtil.prefixed("§d[DM → " + toUsername + "] §7: §f" + text));
				}));
	}

	// Party membership itself is the trust boundary here, no separate friend requirement.
	public static void sendPartyBroadcast(Minecraft client, String text) {
		List<String> recipients = PartyTracker.getMembers().stream()
				.filter(uuid -> !uuid.equals(client.player.getUUID()))
				.filter(PresenceManager::isModUser)
				.map(UUID::toString)
				.toList();
		if (recipients.isEmpty()) {
			client.player.sendSystemMessage(ChatUtil.prefixed(Lang.c("mellooessentials.chat.party.nobody_running_mod")));
			return;
		}
		String selfName = client.player.getGameProfile().name();
		ModAuthManager.getIdentity(client)
				.thenCompose(identity -> ApiClient.sendRelayPartyMessage(recipients, text, identity))
				.whenComplete((ok, error) -> Minecraft.getInstance().execute(() -> {
					if (client.player == null) {
						return;
					}
					if (error != null || !Boolean.TRUE.equals(ok)) {
						client.player.sendSystemMessage(ChatUtil.prefixed(Lang.c("mellooessentials.chat.party.send_failed")));
						return;
					}
					client.player.sendSystemMessage(ChatUtil.prefixed("§d[Party] §b" + selfName + "§7: §f" + text));
				}));
	}

	// Fire-and-forget, deliberately quiet (no "sent"/"nobody online" chat line) - used for automatic
	// background announcements that already print their own text locally elsewhere.
	public static void sendPartyAnnouncement(Minecraft client, String text) {
		if (client.player == null) {
			return;
		}
		List<String> recipients = PartyTracker.getMembers().stream()
				.filter(uuid -> !uuid.equals(client.player.getUUID()))
				.filter(PresenceManager::isModUser)
				.map(UUID::toString)
				.toList();
		if (recipients.isEmpty()) {
			return;
		}
		ModAuthManager.getIdentity(client)
				.thenCompose(identity -> ApiClient.sendRelayPartyMessage(recipients, text, identity))
				.exceptionally(error -> {
					LOGGER.debug("Party announcement failed: {}", error.getMessage());
					return null;
				});
	}

	// ---- command tree ----

	public static LiteralArgumentBuilder<FabricClientCommandSource> buildChatCommand() {
		return ClientCommands.literal("chat")
				.executes(ctx -> {
					ctx.getSource().sendFeedback(ChatUtil.prefixed(Lang.c("mellooessentials.command.chat.usage")));
					return 1;
				})
				.then(ClientCommands.literal("party")
						.then(ClientCommands.argument("message", StringArgumentType.greedyString())
								.executes(ctx -> {
									sendPartyBroadcast(Minecraft.getInstance(), StringArgumentType.getString(ctx, "message"));
									return 1;
								})))
				.then(ClientCommands.argument("name", StringArgumentType.word())
						.suggests(FriendsManager::suggestFriends)
						.then(ClientCommands.argument("message", StringArgumentType.greedyString())
								.executes(ctx -> {
									sendDirect(Minecraft.getInstance(), StringArgumentType.getString(ctx, "name"), StringArgumentType.getString(ctx, "message"));
									return 1;
								})));
	}
}
