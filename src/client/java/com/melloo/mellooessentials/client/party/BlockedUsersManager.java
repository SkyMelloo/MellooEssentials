package com.melloo.mellooessentials.client.party;

import com.melloo.mellooessentials.client.util.ChatUtil;
import com.melloo.mellooessentials.client.util.Lang;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// A personal, client-side-only block list for party members, never synced to sky.melloo.me.
// Blocking someone auto-kicks them from any party you lead, on join or immediately if already in it.
public final class BlockedUsersManager {
	private static final Path FILE = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("mellooessentials-blocked.txt");
	private static final Set<String> blocked = new LinkedHashSet<>();
	private static boolean loaded = false;

	private BlockedUsersManager() {
	}

	private static synchronized void ensureLoaded() {
		if (loaded) {
			return;
		}
		loaded = true;
		if (!Files.exists(FILE)) {
			return;
		}
		try {
			for (String line : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
				String trimmed = line.trim();
				if (!trimmed.isEmpty()) {
					blocked.add(trimmed);
				}
			}
		} catch (IOException e) {
			// Non-fatal - worst case, the block list starts empty this session.
		}
	}

	private static synchronized void save() {
		try {
			Files.write(FILE, blocked, StandardCharsets.UTF_8);
		} catch (IOException e) {
			// Non-fatal - the in-memory list still works for the rest of this session.
		}
	}

	public static synchronized boolean isBlocked(String username) {
		ensureLoaded();
		return blocked.stream().anyMatch(b -> b.equalsIgnoreCase(username));
	}

	// Returns false if they were already blocked.
	public static synchronized boolean block(String username) {
		ensureLoaded();
		if (isBlocked(username)) {
			return false;
		}
		blocked.add(username);
		save();
		return true;
	}

	// Returns false if they weren't blocked in the first place.
	public static synchronized boolean unblock(String username) {
		ensureLoaded();
		boolean removed = blocked.removeIf(b -> b.equalsIgnoreCase(username));
		if (removed) {
			save();
		}
		return removed;
	}

	public static synchronized List<String> getBlocked() {
		ensureLoaded();
		return new ArrayList<>(blocked);
	}

	public static void blockAndKickIfPresent(Minecraft client, String username) {
		boolean wasNewlyBlocked = block(username);
		client.player.sendSystemMessage(ChatUtil.prefixed(wasNewlyBlocked
				? Lang.c("mellooessentials.chat.block.blocked", username)
				: Lang.c("mellooessentials.chat.block.already_blocked", username)));
		if (PartyTracker.isMember(uuidOf(client, username)) && PartyTracker.isLocalPlayerLeader()) {
			PartyKickQueue.queueKick(username);
		}
	}

	// Best-effort lookup from the local player list; a random never-matching UUID if not found just
	// means the immediate-kick check won't fire (they're still caught on their next join).
	private static java.util.UUID uuidOf(Minecraft client, String username) {
		if (client.getConnection() != null) {
			for (var info : client.getConnection().getOnlinePlayers()) {
				if (info.getProfile().name().equalsIgnoreCase(username)) {
					return info.getProfile().id();
				}
			}
		}
		return java.util.UUID.randomUUID();
	}

	// ---- command tree ----

	public static LiteralArgumentBuilder<FabricClientCommandSource> buildBlockCommand() {
		return ClientCommands.literal("block")
				.executes(ctx -> {
					sendBlockedList(ctx.getSource());
					return 1;
				})
				.then(ClientCommands.argument("name", StringArgumentType.word())
						.suggests(BlockedUsersManager::suggestPartyAndPlayers)
						.executes(ctx -> {
							String username = StringArgumentType.getString(ctx, "name");
							blockAndKickIfPresent(Minecraft.getInstance(), username);
							return 1;
						}));
	}

	public static LiteralArgumentBuilder<FabricClientCommandSource> buildUnblockCommand() {
		return ClientCommands.literal("unblock")
				.executes(ctx -> {
					ctx.getSource().sendFeedback(ChatUtil.prefixed(Lang.c("mellooessentials.command.unblock.usage")));
					return 1;
				})
				.then(ClientCommands.argument("name", StringArgumentType.word())
						.suggests(BlockedUsersManager::suggestBlocked)
						.executes(ctx -> {
							String username = StringArgumentType.getString(ctx, "name");
							boolean removed = unblock(username);
							ctx.getSource().sendFeedback(ChatUtil.prefixed(removed
									? Lang.c("mellooessentials.chat.unblock.unblocked", username)
									: Lang.c("mellooessentials.chat.unblock.not_blocked", username)));
							return 1;
						}));
	}

	private static void sendBlockedList(FabricClientCommandSource source) {
		List<String> list = getBlocked();
		if (list.isEmpty()) {
			source.sendFeedback(ChatUtil.prefixed(Lang.c("mellooessentials.command.block.empty")));
			return;
		}
		source.sendFeedback(ChatUtil.prefixed(Lang.c("mellooessentials.command.block.header", list.size())));
		for (String username : list) {
			source.sendFeedback(ChatUtil.prefixed(Lang.c("mellooessentials.command.block.entry", username, username)));
		}
	}

	private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestBlocked(
			com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> ctx,
			com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(getBlocked(), builder);
	}

	private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestPartyAndPlayers(
			com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> ctx,
			com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
		Minecraft client = Minecraft.getInstance();
		List<String> names = new ArrayList<>();
		if (client.getConnection() != null) {
			for (var info : client.getConnection().getOnlinePlayers()) {
				if (PartyTracker.isMember(info.getProfile().id())) {
					names.add(info.getProfile().name());
				}
			}
		}
		return SharedSuggestionProvider.suggest(names, builder);
	}
}
