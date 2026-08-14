# MellooEssentials Changelog

Internal dev version history - every entry below used to live as a giant stacked comment directly above `mod_version` in `gradle.properties`. Moved here since that file was getting absurd. See gradle.properties for the versioning scheme (PATCH/MINOR/MAJOR) and the still-separate PUBLIC_VERSIONING note.

> Versioning scheme, same discipline and starting point as skymelloo (kept as a fully separate counter - this mod's mod_version has nothing to do with skymelloo's own numbers): mod_version below is the INTERNAL/dev version, bumped on every single change so every build has a unique, distinguishable identity - without this, "is the jar I installed actually the latest build" is impossible to answer just by looking at it, which is exactly the confusion that prompted adding this scheme. PATCH (3rd number) for small changes, MINOR (2nd number, patch reset to 0) for bigger features, MAJOR only on explicit instruction. public_version is a separate, hand-maintained user-facing release number, only bumped when a build is actually promoted to public via the sky.melloo.me admin panel.

## 0.13.1 (from 0.13.0) · patch

`cloudSyncEnabled` now defaults to off instead of on (T53). Previously, cross-device sync of HUD positions/cosmetics started automatically the moment an account was linked, with no separate explicit step - now it needs its own opt-in in Settings, same privacy-first bar `presenceSharingEnabled` already had. Existing users who already had it on keep their current setting - this only changes what a fresh install/config starts with.

## 0.13.0 (from 0.12.4) · minor

ModVersionManager now checks SkyMelloo too (if installed), not just itself - Fabric Loader's mod-container registry is global, so this mod can read/hash SkyMelloo's own jar the same way it reads its own, no dependency-direction issue. Fixes a real annoyance: both mods used to run their own completely separate version/integrity check and each fire their own "unofficial build" notice back to back on join. SkyMelloo's own copy of this whole system is deleted - "/sm version"/"/sm legal" now read the new `getSkyMellooXxx()`/`checkSkyMellooNow()` getters here instead.

## 0.12.4 (from 0.12.3) · patch

Added GitHub issue templates (bug report, feature request) and a PR template under `.github/` - closes out most of the repo's Community Profile checklist. No functional change.

## 0.12.3 (from 0.12.2) · patch

CHANGELOG.md readability pass - each entry now shows its patch/minor/major badge next to the version heading instead of buried in the prose. No functional change.

## 0.12.2 (from 0.12.1) · patch

README banner - no public release planned yet, still full active development, Discord contact for anyone interested in helping. No functional change.

## 0.12.1 (from 0.12.0) · patch

Moved the whole per-version changelog history out of gradle.properties (it had grown to 240+ lines) into this file. No functional change.

## 0.12.0 (from 0.11.6) · minor

New EssentialsConfig#presenceSharingEnabled (General tab, "Sharing & Privacy" section) - master switch for presence reporting, moved here from SkyMelloo (was presenceSharingEnabled there, same semantics, but that copy was never actually wired to anything - this one genuinely gates PresenceManager#reportSelf now: off means no presence report is sent at all, not just a reduced one). SkyMelloo hooks into the same shared report loop as before, so this one switch covers both mods' reporting. Custom status text is no longer self-reported by the mod at all (was SkyMelloo's customStatusText) - status is now expected to come from the linked sky.melloo.me account's own status field instead (website task filed to make the server derive it that way for presence queries).

## 0.11.6 (from 0.11.5) · patch

Settings screen (key H) now paints a thin full-screen tint (0x30000000, same value as SkyMelloo's own settings screen) before its popup card, so the area outside the card actually reads as transparent/see-through instead of falling through to the engine's own un-overridden background default. Also removed the manual Push Now/Pull Now buttons from the Cloud tab - push already happens automatically on every settings close, pull happens on its own schedule, so the buttons were redundant.

## 0.11.5 (from 0.11.4) · patch

Comment fix (Magical Power -> Accessory Power, Hypixel's own rename) in SocialMenuScreen's doc comment. No functional change.

## 0.11.4 (from 0.11.3) · patch

New dungeonSyncEnabled field in the regular presence report (T38, filed by the website session) - independent of the dungeonSync payload, which is only ever sent while actually in a dungeon, so the website had no way to tell "sync on but idle" from "genuinely off". New PresenceManager#setDungeonSyncEnabledSupplier extension point, SkyMelloo registers it from its own dungeonSyncEnabled config field.

## 0.11.3 (from 0.11.2) · patch

Comment cleanup - trimmed several long comments down to 1-2 lines. No functional change.

## 0.11.2 (from 0.11.1) · patch

Real bug, root-caused by the website session inspecting an actual stored dungeon replay - the presence report interval was 2s despite SkyMelloo's DungeonSyncManager assuming 1s for its position-history send window, leaving a real ~1s gap with zero recorded position samples between every pair of consecutive reports, throughout every run. Now a real 1s cadence. Doubles presence-report traffic for every online user (was 0.5 req/s/uuid, now 1 req/s) - confirmed still comfortably within the 500 req/min per-uuid rate limit.

## 0.11.1 (from 0.11.0) · patch

Real bug - "/mes verify" confirmation said "You're now an admin on sky.melloo.me", which is wrong - linking an account doesn't grant admin, that's a separate role grant. Now says "Account linked to sky.melloo.me!" or, once the server sends the linked account's display name (filed as T35, not shipped yet), "You're now linked to <name> on sky.melloo.me."

## 0.11.0 (from 0.10.2) · minor

"/mes roll" - Roll (dice/party-pick/word-raffle) moved over from SkyMelloo's "/sm roll", along with the TickDelay/PartyChatSender utilities it needs - never actually SkyBlock-specific, so it belongs with the other generic party tools here. SkyMelloo keeps its own separate copies of TickDelay/PartyChatSender (still used by dungeon-run announcements).

## 0.10.2 (from 0.10.1) · patch

Renamed the short command alias "/me" -> "/mes" - it collided with vanilla's own "/me <action>" roleplay command, silently swallowing it since a client-side command intercepts input before it ever reaches the server. Also: SettingsScreen no longer suppresses vanilla's blur background - it used to override extractBackground to do nothing, leaving the world visible (and often looking tinted light-blue) behind the panel instead of the same dark blur SkyMelloo's own settings screen already gets for free by not overriding it.

## 0.10.0 (from 0.9.9) · minor

New "/me version" and "/me legal" commands, ported over from SkyMelloo's own "/sm version"/"/sm legal" (new ModVersionManager, ApiClient.checkVersion/ fetchLegalInfo). MellooEssentials-only installs never had a version/integrity check or a way to see the legal pages before. version-check hits its own MellooEssentials-specific server route (already live); legal shares SkyMelloo's route, which needs a small server-side fix (filed) to also trust MellooEssentials-signed builds - until then it'll show the "not official" fallback even on a genuinely official build. 0.10.0 collided with an already-registered build from earlier project history - bumped straight to 0.10.1 instead.

## 0.9.9 (from 0.9.8) · patch

Real crash fix - a client-side crash report (skyblock-3 profile, unrelated to the currently-installed version there) showed RollingStats#worstAverage throwing ConcurrentModificationException on the render thread while TpsEstimator's packet-handler mixin added a sample from the client/game thread at the same time - ArrayDeque isn't safe for that. RollingStats' methods are all synchronized now (also fixes FpsMonitor/ServerPingMonitor, which share the same class).

## 0.9.8 (from 0.9.7) · patch

Swapped in the banner's no-subtitle version. No functional change.

## 0.9.7 (from 0.9.6) · patch

README banner image (.github/banner.png). No functional change.

## 0.9.6 (from 0.9.5) · patch

README polish - badge row (license/Minecraft/loader/website). No functional change.

## 0.9.5 (from 0.9.4) · patch

Real report - the role-badge headline visibly stuck out past ConnectionStatusHud's own box on the right side. Right-hand padding widened +30 -> +42.

## 0.9.4 (from 0.9.3) · patch

0.9.3's lobby fallback wasn't enough - a hub/lobby has no mode/map at all (those are per-game concepts), confirmed by decompiling the Hypixel Mod API jar: ClientboundLocationPacket also carries a serverType (LobbyType/GameType), whose getName() gives "Main Lobby" etc. Area line and presence location now try map -> server-type name -> mode.

## 0.9.3 (from 0.9.2) · patch

PlayerInfoHud's Area line now falls back to the Hypixel mode (e.g. "LOBBY") when there's no map name, so it shows up in a hub/lobby too, not just SkyBlock/dungeons. ConnectionStatusHud's admin badge now shows the actual role text ("Connected as Owner") instead of a bare star - setAdminBadgeSupplier takes a Supplier<String> role-label now, not a BooleanSupplier.

## 0.9.2 (from 0.9.1) · patch

Real bugfix - the presence-consolidation change in 0.9.0 made this mod's own report the only one that ever reaches the network, but it always sent X-MellooEssentials-Client regardless of whether SkyMelloo was contributing - so the server's only signal for "this uuid is running SkyMelloo" (the mod-user marker's pink-vs-light-blue choice) went permanently false for every SkyMelloo user. reportPresence now sends X-SkyMelloo-Client instead when SkyMelloo has registered (new PresenceManager#setSkyMellooInstalled).

## 0.9.1 (from 0.9.0) · patch

Comment cleanup - trimmed several long doc comments down to 1-2 lines. No functional change.

## 0.9.0 (from 0.8.5) · minor

This mod's PresenceManager is now the single presence report/query loop for both mods, with real data it never sent before - accountLinked (new AccountLinkStatus, polls /permissions), afk (AfkDetector, moved from SkyMelloo), and location (already-tracked HypixelLocationTracker data, just never wired into the report). Previously this mod's own report always sent empty status/afk/accountLinked and no location at all, which - since SkyMelloo ran a SECOND independent report loop against the exact same endpoint - meant this mod's own report silently overwrote SkyMelloo's richer data with those empty defaults on every tick, a real data-corruption race, not just a missing-feature gap. New extension points (setStatusTextSupplier/setDungeonSyncSupplier/setExtraCosmeticsSupplier/setDungeonSyncListener/ setReportCompletionListener) let SkyMelloo contribute its own data into this single report/query cycle instead of running a duplicate one.

## 0.8.5 (from 0.8.4) · patch

Migrated every ApiClient call from the internal /api/mod/* path to the versioned public /api/public/mod/v1/* surface (see DEVELOPER_API.md) - the internal path is now official-mod-only. Signing unchanged (still Ed25519), only the base URL and signed path prefix moved. No behavior/response-shape change otherwise.

## 0.8.4 (from 0.8.3) · patch

I18n sweep - every hardcoded chat/command/GUI/HUD string now routes through Component.translatable (new Lang util) with the English text moved into en_us.json, instead of Component.literal/raw String. No functional or visual change.

## 0.8.3 (from 0.8.2) · patch

Dead-code sweep (T16) - removed 2 confirmed-unused public methods (MellooEssentialsClient#getOpenSettingsKey, FriendsManager#findFriend), each verified with a repo-wide grep before removal. Kept PartyKickQueue#setExtraJoinAction (a documented, intentional extension point, not leftover cruft). Same-named classes shared with SkyMelloo (ChatUtil/ PartyTracker/etc.) were checked and confirmed intentionally divergent, not duplicates - left untouched. No functional change.

## 0.8.2 (from 0.8.1) · patch

README Building section trimmed further - removed maintainer-only release-process details (admin panel promotion, reportBuild/uploadJar, tokens), added the previously-undocumented node scripts/build.js helper script. No functional change.

## 0.8.1 (from 0.8.0) · patch

Comment/README cleanup pass - trimmed long comments that narrated implementation history/past bugs down to 1-2 lines of current behavior, and shortened the README's Features/Build sections. No functional change.

## 0.8.0 (from 0.7.0) · minor

Friend Highlighting moved here from SkyMelloo, completing the same consolidation staff/party already went through (a duplicate-mixin race between the two mods' glow decisions, one deciding, one always losing). New EssentialsConfig#friendHighlightEnabled/ friendHighlightColor/friendGlowOutlineEnabled (friend stays user-configurable, unlike staff/party's fixed colors - "which color represents MY friends to ME" is a personal preference, not a shared fact). New "Friend Highlighting" row in the General tab opening a new HighlightColorScreen popup (enabled/outline toggles + a 16-color grid, no particle-kind concept - not a cosmetic effect). HighlightManager#getFixedColor/colorizeTabListName now also take a username (needed for the friend check in the Tab-list path, which only had a uuid before).

## 0.7.0 (from 0.6.0) · minor

New /me verify <code>, moved here from SkyMelloo's own "/skymelloo verify" - the admin account-linking flow. The server-side check (/mod/verify) was already mod-agnostic (any mod's valid signature works), so this was purely a client-side command registration move - SkyMelloo hard-depends on this mod already, no reason to keep a duplicate.

## 0.6.0 (from 0.5.1) · minor

New Cloud Sync for this mod's own settings (HUD positions, cosmetics) - real bug report: HUD element positions set in one Lunar Client profile never showed up in another, because each profile has its own separate config file on disk; a shared-disk-path fix was rejected in favor of a real account-based sync, same architecture as SkyMelloo's own CloudSyncManager (cloud unconditionally authoritative on join, no timestamp/diff comparison - see its own doc comment for the full reasoning history). New EssentialsConfig#cloudSyncEnabled (on by default), new ApiClient#fetchPermissions/fetchCloudSettings/pushCloudSettings hitting the same /mod/settings and /mod/permissions routes SkyMelloo already uses - the server already tells the two mods' presence reports apart by request header, so /mod/settings needs the same per-mod namespacing server-side before this can actually round-trip without colliding with SkyMelloo's own cloud data (filed as a task for the website session). New "Cloud" tab in SettingsScreen (toggle + Push Now/Pull Now), pushes on SettingsScreen/HudLayoutEditorScreen close, pulls once per launch on join.

## 0.5.1 (from 0.5.0) · patch

The main menu's rotating panorama is now replaced with a static custom background image (new TitleBackgroundMixin into vanilla TitleScreen#extractBackground, stretched full-screen via GuiGraphicsExtractor#blit) - a user-supplied screenshot, packaged at assets/mellooessentials/textures/gui/title_background.png, downscaled to 800px wide to keep the jar small (a 3840x2160 source made the jar too big for sky.melloo.me's upload limit - hit a real 413 on the first attempt at 1920px).

## 0.5.0 (from 0.4.6) · minor

Took over full ownership of the party block/kick system, moved here from SkyMelloo as a generic, extensible primitive any mod can build on - new party.BlockedUsersManager (personal client-side block list, file-persisted, /me block <name>/unblock <name>) and new party.PartyKickQueue (the join-time chat prompt with [Kick]/[Block] buttons, the cooldown-aware kick queue, and a new setExtraJoinAction extension point for other mods to append to the join line). HighlightManager gained setPartyBlinkColorOverride so SkyMelloo's low-HP party blink survives the consolidation - a narrow color-value callback, not a second glow-decision system (same race SkyMelloo's old duplicate STAFF branch had). Also: colorizeName's party/friend name suffix is now a real heart icon (vanilla's own HUD heart sprite, tinted to the category color) instead of the small "♥" text glyph.

## 0.4.6 (from 0.4.5) · minor

The HUD layout editor (key J) moved here from SkyMelloo entirely - real bugfix, it didn't work at all when only this mod was installed (no J-menu of its own existed). Now unconditionally bound here (same "always bind" pattern as G/H), natively covering the two elements this mod renders (Connection Status, Player Info) - new HudLayoutEditorScreen#setExtraElementsProvider/setExtraSaveHandler let SkyMelloo supply its own elements (Fishing Combo, Party, Dungeon Score, etc.) without this mod needing to know it exists.

## 0.4.5 (from 0.4.4) · minor

SuggestOnlinePlayers now filters out Hypixel NPCs (names starting with "!") and the local player's own name, and merges in the last 10 usernames actually typed into a friend/chat command (new RecentUsernames, session-only) so someone who just logged off is still one tab-complete away. The "/me chat <name>" argument also gained suggestions for the first time (off the friends list, since sendDirect requires one anyway).

## 0.4.4 (from 0.4.3) · patch

Real bugfix - Skyblocker was wrongly listed as a required dependency here (added by mistake for T2). This mod has no Skyblocker integration at all - that's SkyMelloo's own SkyblockerBridge. Removed from fabric.mod.json's depends block; SkyMelloo's own manifest gained it instead.

## 0.4.3 (from 0.4.2) · patch

ConnectionStatusHud's status dot - live report that it looked barely round and its halo nearly touched both the accent stripe and the text at peak pulse size. Bumped the radii up (5-7/3-4, was 4-6/2-3 - larger radii approximate a circle more smoothly via fillCircle's scanlines) and widened the text gutter from 10px to 20px for real clearance on both sides.

## 0.4.2 (from 0.4.1) · patch

Real bug fix - the mod-user marker's pink-vs-light-blue choice was backwards from either side (a MellooEssentials-only player always looked pink to a SkyMelloo client, a real SkyMelloo user always looked light-blue to an Essentials-only client), because both mods report presence to the same /presence endpoint and "is a mod user" alone can't distinguish which mod a given uuid is running. PresenceManager now tracks a new isSkyMelloo(uuid) signal (server-resolved from which mod's client header showed up on that uuid's last presence report - needs a matching server-side change, see the site's own changelog) and both marker mixins use it directly. Removed ModMarkerManager's now-unnecessary/buggy setSpriteOverride hook entirely (SkyMelloo's side removed to match).

## 0.4.1 (from 0.4.0) · patch

Fabric.mod.json's depends block now actually includes Skyblocker (matches what sky.melloo.me's download page already claimed as required - manifest was out of sync with reality). Also fixed the homepage contact field, which still pointed at the old /mellooessentials URL instead of /download.

## 0.4.0 (from 0.3.2) · minor

Took over full ownership of staff/party glow highlighting (SkyMelloo's old duplicate STAFF branch was racing this mod's own mixin for the same vanilla methods - retired on SkyMelloo's side). Moved the entire Friends system (friend list, relay chat, the Social menu/key G) and the "encountered staff" tracker/command here from SkyMelloo - new ApiClient friend/relay/ staff-encounter methods (same server routes, zero backend changes), new ChatUtil, new SocialMenuScreen (Friends only - the old Party column stayed SkyMelloo-only, it already has its own passive Party HUD and commands for that). New /mellooessentials + /me command tree (friend, chat, hitstaff, help). PlayerTabOverlayMixin gained throttled diagnostic logging for a live report that the tab-list marker doesn't show on some clients - no functional change, chasing a bug that reads correctly in source.

## 0.3.2 (from 0.3.1) · patch

ConnectionStatusHud's status dot is now a real circle (scanline-filled, not a flat square) that gently pulses in size on a 2s cycle, moved to sit directly in front of the "Connected" headline text itself instead of centered in a gutter spanning both text lines.

## 0.3.1 (from 0.3.0) · patch

ConnectionStatusHud redesigned to a fixed, always-exactly-2-line layout (bold headline + one combined detail line, instead of up to 3 separately stacked lines) with a glowing status dot and a colored accent stripe down the panel's left edge. setExtraLineProvider's contract changed slightly - it now supplies a short fragment appended to the detail line (e.g. "42ms"), not a whole separate line already prefixed with "sky.melloo.me" (SkyMelloo's own registration updated to match).

## 0.3.0 (from 0.2.0) · minor

Reversed the H-key deferral above - H now ALWAYS opens this mod's settings screen unconditionally, even with SkyMelloo installed (SkyMelloo's own H binding was removed instead, now unbound by default - one settings surface, no more race). ConnectionStatusHud gained two extension points (setAdminBadgeSupplier, setExtraLineProvider) so SkyMelloo's admin badge and sky.melloo.me ping reading still show up in this mod's single status box, since SkyMelloo's own former status/player-info HUDs (byte-for-byte duplicates of this mod's) were removed on its side. Added the missing "Connection Status HUD" toggle checkbox to the General tab (the config field already existed, the checkbox itself never did).

## 0.2.0 (from 0.1.4) · minor

Real new feature batch - PlayerInfoHud and the new ConnectionStatusHud (a "Connected to sky.melloo.me, for Xm Ys" indicator, ticking duration via ModAuthManager's new connection-state tracking) both gained real X/Y position config, draggable via SkyMelloo's own HUD layout editor when it's installed (this mod has no positioning UI of its own). Also: the H keybind now defers to SkyMelloo's own settings screen when SkyMelloo is installed (they collided on the same default key), with a new "SkyMelloo Config" button on this screen as the way back, via a new SettingsScreen.setSkyMellooScreenOpener extension point SkyMelloo registers into.

## 0.1.4 (from 0.1.3) · patch

SettingsScreen gained a second constructor (openToCosmetics) so SkyMelloo's own menu can open straight to the Cosmetics tab here instead of maintaining a second, duplicate cosmetics UI of its own.

## 0.1.3 (from 0.1.2) · patch

Flush-against-the-icon read as too tight - added back a thin space (U+2009), narrower than the original regular space.

## 0.1.2 (from 0.1.1) · patch

Removed the diagnostic chat message again - the "marker missing" report turned out to be because the jars were never in the Lunar profile actually being tested (skyblock-3), not a real bug, so there was nothing left to diagnose. Also removed the space between the marker sprite and the player name (ModMarkerManager#apply) - the name now sits directly against the icon.

## 0.1.1 (from 0.1.0) · patch

Added a one-time chat diagnostic to MellooEssentialsClient's tick loop (isModUser(self) reported on join) to track down a live report that the mod-user marker isn't showing on the local player's own nametag/tab row at all, in code that otherwise looks correct on inspection. Temporary - remove once the actual root cause is found.

## 0.1.0 (from 0.0.0) · minor

Same reasoning skymelloo's own first bump used: a real batch of features had already shipped while this sat at a single unbumped version the whole time (the mod-user marker system and its tab-list counterpart), discovered only once this versioning scheme itself was being added - so the correction covers all of it at once.
