package com.melloo.mellooessentials.client;

import com.melloo.mellooessentials.client.api.ApiClient;
import com.melloo.mellooessentials.client.api.ModAuthManager;
import com.melloo.mellooessentials.client.cosmetics.CosmeticsRenderer;
import com.melloo.mellooessentials.client.gui.HudLayoutEditorScreen;
import com.melloo.mellooessentials.client.gui.PlayerInfoHud;
import com.melloo.mellooessentials.client.gui.SettingsScreen;
import com.melloo.mellooessentials.client.gui.SocialMenuScreen;
import com.melloo.mellooessentials.client.gui.FpsMonitor;
import com.melloo.mellooessentials.client.party.BlockedUsersManager;
import com.melloo.mellooessentials.client.party.PartyKickQueue;
import com.melloo.mellooessentials.client.party.PartyTracker;
import com.melloo.mellooessentials.client.social.ConnectionStatusHud;
import com.melloo.mellooessentials.client.social.FriendsManager;
import com.melloo.mellooessentials.client.social.HypixelLocationTracker;
import com.melloo.mellooessentials.client.social.PresenceManager;
import com.melloo.mellooessentials.client.social.RelayChatManager;
import com.melloo.mellooessentials.client.social.StaffEncounterTracker;
import com.melloo.mellooessentials.client.util.ChatUtil;
import com.melloo.mellooessentials.client.util.CloudSyncManager;
import com.melloo.mellooessentials.client.util.HypixelDetector;
import com.melloo.mellooessentials.client.util.Lang;
import com.melloo.mellooessentials.client.util.ModVersionManager;
import com.melloo.mellooessentials.client.util.ServerPingMonitor;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class MellooEssentialsClient implements ClientModInitializer {
	public static final String MOD_ID = "mellooessentials";

	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(MOD_ID, "main")
	);

	private static KeyMapping openSettingsKey;
	private static KeyMapping socialMenuKey;
	private static KeyMapping hudLayoutKey;

	@Override
	public void onInitializeClient() {
		openSettingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.mellooessentials.open_settings",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_H,
				CATEGORY
		));

		// Opens the Social menu (friends list, see SocialMenuScreen) - defaults to G (free in vanilla).
		socialMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.mellooessentials.social_menu",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_G,
				CATEGORY
		));

		// Moved here from SkyMelloo - this mod owns the HUD layout editor unconditionally now (same
		// "always bind, don't defer" pattern as G/H above), natively covering only the two HUD
		// elements this mod actually renders. SkyMelloo hooks its own extra elements in via
		// HudLayoutEditorScreen.setExtraElementsProvider when it's installed - see its own doc comment.
		hudLayoutKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.mellooessentials.hud_layout",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_J,
				CATEGORY
		));

		PartyTracker.init();
		PartyKickQueue.init();
		HypixelLocationTracker.init();

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "player_info"), PlayerInfoHud.INSTANCE);
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "connection_status"), ConnectionStatusHud.INSTANCE);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// H always opens this mod's settings screen, whether or not SkyMelloo is also installed -
			// this is the single settings/status/player-info surface for both mods now (SkyMelloo's
			// own settings screen no longer binds a default key at all, reachable instead via the
			// "SkyMelloo Config" button this screen shows when SkyMelloo is installed).
			while (openSettingsKey.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new SettingsScreen(null));
				}
			}
			while (socialMenuKey.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new SocialMenuScreen());
				}
			}
			while (hudLayoutKey.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new HudLayoutEditorScreen());
				}
			}

			// Runs regardless of server - these measure the actual connection/game itself, or (for
			// Friends/relay chat/staff-encounter tracking) are meant to keep working anywhere, not
			// anything Hypixel-specific.
			FpsMonitor.tick(client);
			ServerPingMonitor.tick(client);
			FriendsManager.tick(client);
			RelayChatManager.tick(client);
			StaffEncounterTracker.tick(client);
			CloudSyncManager.pullIfNeeded(client);

			// Everything else is Hypixel-only - no reason to run party tracking/cosmetics/presence on
			// any other server.
			if (!HypixelDetector.isHypixel(client)) {
				return;
			}

			PartyTracker.tick();
			CosmeticsRenderer.tick(client);
			PresenceManager.tick(client);
			ModVersionManager.checkOnce(client);
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			var mellooessentialsNode = dispatcher.register(
					ClientCommands.literal("mellooessentials")
							.executes(ctx -> {
								sendHelp(ctx.getSource());
								return 1;
							})
							.then(ClientCommands.literal("help").executes(ctx -> {
								sendHelp(ctx.getSource());
								return 1;
							}))
							.then(FriendsManager.buildFriendCommand())
							.then(RelayChatManager.buildChatCommand())
							.then(BlockedUsersManager.buildBlockCommand())
							.then(BlockedUsersManager.buildUnblockCommand())
							// Named after the German "Staff getroffen" ("met/encountered staff") - a running
							// list of every real staff/owner member you've ever shared a tab list with,
							// anywhere (see StaffEncounterTracker, which keeps scanning regardless of server).
							// Moved here from SkyMelloo's "/sm hitstaff".
							.then(ClientCommands.literal("hitstaff").executes(ctx -> {
								var source = ctx.getSource();
								Minecraft client = Minecraft.getInstance();
								ModAuthManager.getIdentity(client)
										.exceptionally(error -> null)
										.thenCompose(ApiClient::fetchStaffEncounters)
										.thenAccept(encounters -> client.execute(() -> {
											if (encounters.isEmpty()) {
												source.sendFeedback(ChatUtil.prefixed(Lang.c("mellooessentials.command.hitstaff.empty")));
												return;
											}
											var sorted = new java.util.ArrayList<>(encounters);
											sorted.sort((a, b) -> Long.compare(b.lastSeenMillis(), a.lastSeenMillis()));
											source.sendFeedback(ChatUtil.prefixed(Lang.c("mellooessentials.command.hitstaff.header", sorted.size())));
											long now = System.currentTimeMillis();
											for (var entry : sorted) {
												String roleLabel = switch (entry.role()) {
													case "owner" -> Lang.s("mellooessentials.role.owner");
													case "admin" -> Lang.s("mellooessentials.role.admin");
													case "developer" -> Lang.s("mellooessentials.role.developer");
													case "moderator" -> Lang.s("mellooessentials.role.moderator");
													default -> entry.role();
												};
												String displayName = entry.websiteDisplayName() != null ? entry.websiteDisplayName() : entry.username();
												source.sendFeedback(ChatUtil.prefixed(Lang.c("mellooessentials.command.hitstaff.entry", roleLabel, displayName, formatAgo(now - entry.lastSeenMillis()))));
											}
										}));
								return 1;
							}))
							// Admin account verification - the server-side check is mod-agnostic, any mod's
							// valid signature works.
							.then(ClientCommands.literal("verify")
									.executes(ctx -> {
										ctx.getSource().sendFeedback(ChatUtil.prefixed(Lang.c("mellooessentials.command.verify.usage")));
										return 1;
									})
									.then(ClientCommands.argument("code", StringArgumentType.word()).executes(ctx -> {
										String code = StringArgumentType.getString(ctx, "code");
										var source = ctx.getSource();
										Minecraft client = Minecraft.getInstance();
										if (client.player == null) {
											return 1;
										}
										source.sendFeedback(ChatUtil.prefixed(Lang.c("mellooessentials.command.verify.checking")));
										ModAuthManager.getIdentity(client).thenCompose(identity -> ApiClient.verifyAccount(code, identity))
												.whenComplete((result, error) ->
														Minecraft.getInstance().execute(() -> {
															Minecraft c = Minecraft.getInstance();
															if (c.player == null) {
																return;
															}
															if (error != null) {
																c.player.sendSystemMessage(ChatUtil.prefixed(Lang.c("mellooessentials.chat.verify.connection_failed", ChatUtil.friendlyError(error))));
															} else if (result.ok()) {
																c.player.sendSystemMessage(ChatUtil.prefixed(Lang.c("mellooessentials.chat.verify.linked")));
															} else {
																c.player.sendSystemMessage(ChatUtil.prefixed(Lang.c("mellooessentials.chat.verify.failed", result.error())));
															}
														})
												);
										return 1;
									})))
							.then(ClientCommands.literal("version").executes(ctx -> {
								String version = ModVersionManager.getLocalVersion();
								String publicVersion = ModVersionManager.getPublicVersion();
								String jarHash = ModVersionManager.getLocalJarHash();
								ctx.getSource().sendFeedback(ChatUtil.prefixed(Lang.c("mellooessentials.command.version.header")));
								net.minecraft.network.chat.Component jarHashText = jarHash != null ? net.minecraft.network.chat.Component.literal(jarHash) : Lang.c("mellooessentials.command.version.jarhash_unknown");
								ctx.getSource().sendFeedback(ChatUtil.prefixed(Lang.c("mellooessentials.command.version.running", publicVersion, version, jarHashText)));
								ctx.getSource().sendFeedback(ChatUtil.prefixed(Lang.c("mellooessentials.command.version.checking")));
								ModVersionManager.checkNow(
										result -> {
											Minecraft c = Minecraft.getInstance();
											if (c.player == null) {
												return;
											}
											if (result == null) {
												c.player.sendSystemMessage(ChatUtil.prefixed(Lang.c("mellooessentials.command.version.unreachable")));
												return;
											}
											if (result.latestPublicVersion() != null) {
												c.player.sendSystemMessage(ChatUtil.prefixed(Lang.c("mellooessentials.command.version.latest_published", result.latestPublicVersion())));
											}
											if (result.upToDate()) {
												c.player.sendSystemMessage(ChatUtil.prefixed(Lang.c("mellooessentials.command.version.up_to_date")));
											} else {
												c.player.sendSystemMessage(ChatUtil.prefixed(Lang.c("mellooessentials.command.version.outdated")));
											}
											c.player.sendSystemMessage(legalLink(Lang.c("mellooessentials.command.version.get_from_official"), "https://sky.melloo.me/download"));
										},
										cooldownSeconds -> {
											Minecraft c = Minecraft.getInstance();
											if (c.player == null) {
												return;
											}
											c.player.sendSystemMessage(ChatUtil.prefixed(Lang.c("mellooessentials.command.version.cooldown", cooldownSeconds)));
											c.player.sendSystemMessage(legalLink(Lang.c("mellooessentials.command.version.get_from_official"), "https://sky.melloo.me/download"));
										}
								);
								return 1;
							}))
							// Same reasoning as SkyMelloo's "/sm legal" - fetched server-side, gated by the
							// same build-verification check the integrity system already does.
							.then(ClientCommands.literal("legal").executes(ctx -> {
								String jarHash = ModVersionManager.getLocalJarHash();
								ApiClient.fetchLegalInfo(jarHash).whenComplete((info, error) -> Minecraft.getInstance().execute(() -> {
									if (error != null || info == null) {
										var lastResult = ModVersionManager.getLastResult();
										net.minecraft.network.chat.Component maintainer = lastResult != null && lastResult.maintainerUsername() != null
												? net.minecraft.network.chat.Component.literal(lastResult.maintainerUsername())
												: Lang.c("mellooessentials.command.legal.fallback_maintainer");
										ctx.getSource().sendFeedback(ChatUtil.prefixed(Lang.c("mellooessentials.command.legal.not_official")));
										ctx.getSource().sendFeedback(ChatUtil.prefixed(Lang.c("mellooessentials.command.legal.fork_reminder", maintainer)));
										return;
									}
									ctx.getSource().sendFeedback(ChatUtil.prefixed(Lang.c("mellooessentials.command.legal.header")));
									ctx.getSource().sendFeedback(legalLink(Lang.c("mellooessentials.command.legal.label_imprint"), info.imprint()));
									ctx.getSource().sendFeedback(legalLink(Lang.c("mellooessentials.command.legal.label_privacy"), info.privacy()));
									ctx.getSource().sendFeedback(legalLink(Lang.c("mellooessentials.command.legal.label_terms"), info.terms()));
								}));
								return 1;
							}))
			);
			// "me" collided with vanilla's own "/me <action>" roleplay command - a client-side
			// command with the same name intercepts the input before it can ever reach the server,
			// silently breaking vanilla's "/me" for everyone using this mod. "mes" doesn't collide
			// with anything.
			dispatcher.register(ClientCommands.literal("mes").redirect(mellooessentialsNode));
		});
	}

	/** Clickable "§dLabel: §fhttps://..." chat line - opens the URL in the system browser. Used by {@code /mes legal} and {@code /mes version}'s download reminder. */
	private static net.minecraft.network.chat.MutableComponent legalLink(net.minecraft.network.chat.Component label, String url) {
		return Lang.c("mellooessentials.command.legal.link_line", label, url).withStyle(style -> style
				.withClickEvent(new net.minecraft.network.chat.ClickEvent.OpenUrl(java.net.URI.create(url)))
				.withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(Lang.c("mellooessentials.command.legal.hover_open_browser"))));
	}

	private static void sendHelp(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source) {
		source.sendFeedback(ChatUtil.prefixed(Lang.c("mellooessentials.command.help.header")));
		source.sendFeedback(ChatUtil.prefixed(Lang.c("mellooessentials.command.help.friend")));
		source.sendFeedback(ChatUtil.prefixed(Lang.c("mellooessentials.command.help.chat")));
		source.sendFeedback(ChatUtil.prefixed(Lang.c("mellooessentials.command.help.hitstaff")));
		source.sendFeedback(ChatUtil.prefixed(Lang.c("mellooessentials.command.help.block")));
		source.sendFeedback(ChatUtil.prefixed(Lang.c("mellooessentials.command.help.verify")));
		source.sendFeedback(ChatUtil.prefixed(Lang.c("mellooessentials.command.help.version")));
		source.sendFeedback(ChatUtil.prefixed(Lang.c("mellooessentials.command.help.legal")));
	}

	/** Rough "X ago" duration for /mes hitstaff - coarsest unit only (a last-seen from 2 days ago doesn't need minute precision). */
	private static String formatAgo(long millisAgo) {
		long seconds = millisAgo / 1000;
		if (seconds < 60) {
			return Lang.s("mellooessentials.time.moments");
		}
		long minutes = seconds / 60;
		if (minutes < 60) {
			return Lang.s(minutes == 1 ? "mellooessentials.time.minute" : "mellooessentials.time.minutes", minutes);
		}
		long hours = minutes / 60;
		if (hours < 24) {
			return Lang.s(hours == 1 ? "mellooessentials.time.hour" : "mellooessentials.time.hours", hours);
		}
		long days = hours / 24;
		return Lang.s(days == 1 ? "mellooessentials.time.day" : "mellooessentials.time.days", days);
	}

	/**
	 * Online players (real ones - Hypixel NPCs, whose names all start with "!", are filtered out, and
	 * so is the local player's own name) PLUS the last 10 usernames actually typed into a friend/chat
	 * command ({@link com.melloo.mellooessentials.client.social.RecentUsernames}), merged and
	 * deduplicated - so someone who just logged off (or an NPC-free retype of a name you used a
	 * minute ago) is still one tab-complete away instead of needing the exact spelling again.
	 */
	public static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestOnlinePlayers(
			com.mojang.brigadier.context.CommandContext<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> ctx,
			com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
		Minecraft client = Minecraft.getInstance();
		if (client.getConnection() == null || client.player == null) {
			return builder.buildFuture();
		}
		String selfName = client.player.getGameProfile().name();
		java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>();
		for (var info : client.getConnection().getOnlinePlayers()) {
			String name = info.getProfile().name();
			if (!name.startsWith("!") && !name.equalsIgnoreCase(selfName)) {
				names.add(name);
			}
		}
		names.addAll(com.melloo.mellooessentials.client.social.RecentUsernames.get());
		return SharedSuggestionProvider.suggest(names.stream(), builder);
	}
}
