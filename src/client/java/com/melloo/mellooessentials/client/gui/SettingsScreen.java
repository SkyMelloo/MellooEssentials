package com.melloo.mellooessentials.client.gui;

import com.melloo.mellooessentials.client.config.EssentialsConfig;
import com.melloo.mellooessentials.client.util.CloudSyncManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Centered, tabbed popup card (same bordered-panel style as SkyMelloo's StringInputScreen).
 * Clicking a cosmetic that actually has options (a color OR a particle choice - never both, see
 * CosmeticEditScreen's own doc comment on why) opens {@link CosmeticEditScreen} for it; one with
 * neither just toggles directly in place.
 */
public class SettingsScreen extends Screen {
	private static final int PANEL_MAX_WIDTH = 420;
	private static final int PANEL_MAX_HEIGHT = 460;
	private static final int ROW_H = 16;
	private static final int ROW_GAP = 2;
	private static final int TAB_HEIGHT = 20;
	private static final int TEXT_ON = 0xFFFFFFFF;
	private static final int TEXT_OFF = 0xFFAAAAAA;
	private static final int ROW_BG_HOVER = 0x14FFFFFF;
	private static final int BORDER_COLOR = 0xFF66DDFF;
	private static final int PANEL_COLOR = 0x99101018; // translucent - the game world stays visible behind the panel

	private enum Tab {
		GENERAL("General"), COSMETICS("Cosmetics"), CLOUD("Cloud");

		final String label;

		Tab(String label) {
			this.label = label;
		}
	}

	private interface RowFactory {
		AbstractWidget create(int x, int y, int w, int h);
	}

	// Static (not per-instance) so the scroll position survives fully closing and reopening the
	// settings menu, not just switching tabs within one open session - there's only ever one of
	// these menus open at a time, so sharing this across instances is safe.
	private static final java.util.Map<Tab, Integer> scrollByTab = new java.util.EnumMap<>(Tab.class);

	// SkyMelloo (which depends on this mod, never the other way around) registers a callback that
	// opens its own settings screen here at startup, so this screen can offer a button back to it
	// without ever referencing SkyMelloo's classes directly - same extension-point pattern as
	// ModMarkerManager#setSpriteOverride. A plain Runnable (not a Supplier<Screen>) since SkyMelloo's
	// own open-settings entry point is itself a self-contained static method that calls
	// Minecraft#setScreen internally (plus a couple of refresh side effects) rather than just
	// constructing a Screen. Null (no button shown) when SkyMelloo isn't installed.
	private static volatile Runnable openSkyMellooScreen = null;

	public static void setSkyMellooScreenOpener(Runnable opener) {
		openSkyMellooScreen = opener;
	}

	private final Screen parent;
	private final List<RowFactory> rows = new ArrayList<>();
	private Tab currentTab = Tab.GENERAL;
	private int scrollOffset = 0;

	public SettingsScreen(Screen parent) {
		super(Component.literal("MellooEssentials"));
		this.parent = parent;
	}

	/** Opens straight to the Cosmetics tab instead of General - used by SkyMelloo's own menu, which links here directly instead of maintaining a second, duplicate cosmetics UI of its own. */
	public SettingsScreen(Screen parent, boolean openToCosmetics) {
		this(parent);
		if (openToCosmetics) {
			currentTab = Tab.COSMETICS;
		}
	}

	private int panelWidth() {
		return Math.min(PANEL_MAX_WIDTH, this.width - 40);
	}

	private int panelHeight() {
		return Math.min(PANEL_MAX_HEIGHT, this.height - 40);
	}

	private static final int PANEL_LEFT_MARGIN = 20;

	private int panelX() {
		return PANEL_LEFT_MARGIN;
	}

	private int panelY() {
		return (this.height - panelHeight()) / 2;
	}

	private int tabBarY() {
		return panelY() + 26;
	}

	private int listTop() {
		return tabBarY() + TAB_HEIGHT + 8;
	}

	private int listBottom() {
		return panelY() + panelHeight() - 36;
	}

	@Override
	protected void init() {
		scrollOffset = scrollByTab.getOrDefault(currentTab, 0);
		buildRowsForCurrentTab();
	}

	private void buildRowsForCurrentTab() {
		rows.clear();
		EssentialsConfig c = EssentialsConfig.get();
		switch (currentTab) {
			case GENERAL -> {
				rows.add(infoRow("Party and team highlighting are always active."));
				rows.add(headerRow("HUD"));
				rows.add(boolRow("Player Info HUD (FPS/ping/TPS/server/area/coords/facing)", () -> c.playerInfoHudEnabled, v -> c.playerInfoHudEnabled = v));
				rows.add(boolRow("Connection Status HUD (sky.melloo.me)", () -> c.connectionStatusHudEnabled, v -> c.connectionStatusHudEnabled = v));
			}
			case COSMETICS -> {
				rows.add(boolRow("Cosmetics Enabled (master switch)", () -> c.cosmeticsEnabled, v -> c.cosmeticsEnabled = v));
				rows.add(actionRow("Set All Colors", () -> Minecraft.getInstance().setScreen(new BulkCosmeticScreen(this, true))));
				rows.add(actionRow("Set All Particle Kinds", () -> Minecraft.getInstance().setScreen(new BulkCosmeticScreen(this, false))));
				rows.add(actionRow("Reset All Cosmetics", () -> {
					EssentialsConfig.get().resetAllCosmetics();
					EssentialsConfig.save();
				}));
				rows.add(colorCosmeticRow("Halo", "halo", () -> c.haloEnabled, v -> c.haloEnabled = v, () -> c.haloColor, v -> c.haloColor = v, () -> c.haloParticleKind, v -> c.haloParticleKind = v));
				rows.add(particleCosmeticRow("Cherry Blossom", "cherryBlossom", () -> c.cherryBlossomEnabled, v -> c.cherryBlossomEnabled = v, () -> c.cherryBlossomParticle, v -> c.cherryBlossomParticle = v));
				rows.add(colorCosmeticRow("Helix", "rainbowHelix", () -> c.rainbowHelixEnabled, v -> c.rainbowHelixEnabled = v, () -> c.rainbowHelixColor, v -> c.rainbowHelixColor = v, () -> c.rainbowHelixParticleKind, v -> c.rainbowHelixParticleKind = v));
				rows.add(colorCosmeticRow("Aura", "aura", () -> c.auraEnabled, v -> c.auraEnabled = v, () -> c.auraColor, v -> c.auraColor = v, () -> c.auraParticleKind, v -> c.auraParticleKind = v));
				rows.add(colorCosmeticRow("Wave", "wave", () -> c.waveEnabled, v -> c.waveEnabled = v, () -> c.waveColor, v -> c.waveColor = v, () -> c.waveParticleKind, v -> c.waveParticleKind = v));
				rows.add(defaultableParticleCosmeticRow("Rain Cloud", "rainCloud", () -> c.rainCloudEnabled, v -> c.rainCloudEnabled = v, () -> c.rainCloudParticleKind, v -> c.rainCloudParticleKind = v));
				rows.add(particleCosmeticRow("Fire Ring", "fireRing", () -> c.fireRingEnabled, v -> c.fireRingEnabled = v, () -> c.fireRingParticle, v -> c.fireRingParticle = v));
				rows.add(particleCosmeticRow("Star Rain", "starRain", () -> c.starRainEnabled, v -> c.starRainEnabled = v, () -> c.starRainParticle, v -> c.starRainParticle = v));
				rows.add(particleCosmeticRow("Spark Aura", "sparkAura", () -> c.sparkAuraEnabled, v -> c.sparkAuraEnabled = v, () -> c.sparkAuraParticle, v -> c.sparkAuraParticle = v));
				rows.add(colorCosmeticRow("Lissajous Curve", "lissajous", () -> c.lissajousEnabled, v -> c.lissajousEnabled = v, () -> c.lissajousColor, v -> c.lissajousColor = v, () -> c.lissajousParticleKind, v -> c.lissajousParticleKind = v));
				rows.add(colorCosmeticRow("Rose Curve", "roseCurve", () -> c.roseCurveEnabled, v -> c.roseCurveEnabled = v, () -> c.roseCurveColor, v -> c.roseCurveColor = v, () -> c.roseCurveParticleKind, v -> c.roseCurveParticleKind = v));
				rows.add(colorCosmeticRow("Landing Shockwave", "landingShockwave", () -> c.landingShockwaveEnabled, v -> c.landingShockwaveEnabled = v, () -> c.landingShockwaveColor, v -> c.landingShockwaveColor = v, () -> c.landingShockwaveParticleKind, v -> c.landingShockwaveParticleKind = v));
				rows.add(particleCosmeticRow("Firework Burst", "fireworkBurst", () -> c.fireworkBurstEnabled, v -> c.fireworkBurstEnabled = v, () -> c.fireworkBurstParticle, v -> c.fireworkBurstParticle = v));
				rows.add(colorCosmeticRow("Frost Aura", "frostAura", () -> c.frostAuraEnabled, v -> c.frostAuraEnabled = v, () -> c.frostAuraColor, v -> c.frostAuraColor = v, () -> c.frostAuraParticleKind, v -> c.frostAuraParticleKind = v));
				rows.add(particleCosmeticRow("Note Melody", "noteMelody", () -> c.noteMelodyEnabled, v -> c.noteMelodyEnabled = v, () -> c.noteMelodyParticle, v -> c.noteMelodyParticle = v));
				rows.add(colorCosmeticRow("Portal Vortex", "portalVortex", () -> c.portalVortexEnabled, v -> c.portalVortexEnabled = v, () -> c.portalVortexColor, v -> c.portalVortexColor = v, () -> c.portalVortexParticleKind, v -> c.portalVortexParticleKind = v));
				rows.add(particleCosmeticRow("Heart Trail", "heartTrail", () -> c.heartTrailEnabled, v -> c.heartTrailEnabled = v, () -> c.heartTrailParticle, v -> c.heartTrailParticle = v));
				rows.add(colorCosmeticRow("Spiral Galaxy", "spiralGalaxy", () -> c.spiralGalaxyEnabled, v -> c.spiralGalaxyEnabled = v, () -> c.spiralGalaxyColor, v -> c.spiralGalaxyColor = v, () -> c.spiralGalaxyParticleKind, v -> c.spiralGalaxyParticleKind = v));
				rows.add(colorCosmeticRow("Jump Trail", "jumpTrail", () -> c.jumpTrailEnabled, v -> c.jumpTrailEnabled = v, () -> c.jumpTrailColor, v -> c.jumpTrailColor = v, () -> c.jumpTrailParticleKind, v -> c.jumpTrailParticleKind = v));
				rows.add(particleCosmeticRow("Totem Flash", "totemFlash", () -> c.totemFlashEnabled, v -> c.totemFlashEnabled = v, () -> c.totemFlashParticle, v -> c.totemFlashParticle = v));
				rows.add(particleCosmeticRow("Sculk Pulse", "sculkPulse", () -> c.sculkPulseEnabled, v -> c.sculkPulseEnabled = v, () -> c.sculkPulseParticle, v -> c.sculkPulseParticle = v));
				rows.add(particleCosmeticRow("Omen Aura", "omenAura", () -> c.omenAuraEnabled, v -> c.omenAuraEnabled = v, () -> c.omenAuraParticle, v -> c.omenAuraParticle = v));
				rows.add(colorCosmeticRow("Gust Aura", "gustAura", () -> c.gustAuraEnabled, v -> c.gustAuraEnabled = v, () -> c.gustAuraColor, v -> c.gustAuraColor = v, () -> c.gustAuraParticleKind, v -> c.gustAuraParticleKind = v));
				rows.add(colorCosmeticRow("Ash Fall", "ashFall", () -> c.ashFallEnabled, v -> c.ashFallEnabled = v, () -> c.ashFallColor, v -> c.ashFallColor = v, () -> c.ashFallParticleKind, v -> c.ashFallParticleKind = v));
				rows.add(defaultableParticleCosmeticRow("Campfire Smoke", "campfireSmoke", () -> c.campfireSmokeEnabled, v -> c.campfireSmokeEnabled = v, () -> c.campfireSmokeParticleKind, v -> c.campfireSmokeParticleKind = v));
				rows.add(colorCosmeticRow("Tornado", "tornado", () -> c.tornadoEnabled, v -> c.tornadoEnabled = v, () -> c.tornadoColor, v -> c.tornadoColor = v, () -> c.tornadoParticleKind, v -> c.tornadoParticleKind = v));
				rows.add(colorCosmeticRow("Black Hole", "blackHole", () -> c.blackHoleEnabled, v -> c.blackHoleEnabled = v, () -> c.blackHoleColor, v -> c.blackHoleColor = v, () -> c.blackHoleParticleKind, v -> c.blackHoleParticleKind = v));
				rows.add(colorCosmeticRow("Twin Vortex", "twinVortex", () -> c.twinVortexEnabled, v -> c.twinVortexEnabled = v, () -> c.twinVortexColor, v -> c.twinVortexColor = v, () -> c.twinVortexParticleKind, v -> c.twinVortexParticleKind = v));
				rows.add(particleCosmeticRow("Enchanted Crit Sparkle", "enchantedCritSparkle", () -> c.enchantedCritSparkleEnabled, v -> c.enchantedCritSparkleEnabled = v, () -> c.enchantedCritSparkleParticle, v -> c.enchantedCritSparkleParticle = v));
				rows.add(particleCosmeticRow("Dust Plume Trail", "dustPlumeTrail", () -> c.dustPlumeTrailEnabled, v -> c.dustPlumeTrailEnabled = v, () -> c.dustPlumeTrailParticle, v -> c.dustPlumeTrailParticle = v));
				rows.add(colorCosmeticRow("Charge Up", "chargeUp", () -> c.chargeUpEnabled, v -> c.chargeUpEnabled = v, () -> c.chargeUpColor, v -> c.chargeUpColor = v, () -> c.chargeUpParticleKind, v -> c.chargeUpParticleKind = v));
				rows.add(colorCosmeticRow("Orbit Rings", "orbitRings", () -> c.orbitRingsEnabled, v -> c.orbitRingsEnabled = v, () -> c.orbitRingsColor, v -> c.orbitRingsColor = v, () -> c.orbitRingsParticleKind, v -> c.orbitRingsParticleKind = v));
				rows.add(particleCosmeticRow("Lightning Aura", "lightningAura", () -> c.lightningAuraEnabled, v -> c.lightningAuraEnabled = v, () -> c.lightningAuraParticle, v -> c.lightningAuraParticle = v));
				rows.add(defaultableParticleCosmeticRow("Confetti Burst", "confettiBurst", () -> c.confettiBurstEnabled, v -> c.confettiBurstEnabled = v, () -> c.confettiBurstParticleKind, v -> c.confettiBurstParticleKind = v));
				rows.add(colorCosmeticRow("Moth Wings", "mothWings", () -> c.mothWingsEnabled, v -> c.mothWingsEnabled = v, () -> c.mothWingsColor, v -> c.mothWingsColor = v, () -> c.mothWingsParticleKind, v -> c.mothWingsParticleKind = v));
				rows.add(colorCosmeticRow("Phoenix Wings", "phoenixWings", () -> c.phoenixWingsEnabled, v -> c.phoenixWingsEnabled = v, () -> c.phoenixWingsColor, v -> c.phoenixWingsColor = v, () -> c.phoenixWingsParticleKind, v -> c.phoenixWingsParticleKind = v));
				rows.add(particleCosmeticRow("Void Rift", "voidRift", () -> c.voidRiftEnabled, v -> c.voidRiftEnabled = v, () -> c.voidRiftParticle, v -> c.voidRiftParticle = v));
				rows.add(particleCosmeticRow("Star Weave", "starWeave", () -> c.starWeaveEnabled, v -> c.starWeaveEnabled = v, () -> c.starWeaveParticle, v -> c.starWeaveParticle = v));
				rows.add(particleCosmeticRow("Ascending Sparkles", "ascendingSparkles", () -> c.ascendingSparklesEnabled, v -> c.ascendingSparklesEnabled = v, () -> c.ascendingSparklesParticle, v -> c.ascendingSparklesParticle = v));
				rows.add(particleCosmeticRow("Comet Trail", "cometTrail", () -> c.cometTrailEnabled, v -> c.cometTrailEnabled = v, () -> c.cometTrailParticle, v -> c.cometTrailParticle = v));
				rows.add(particleCosmeticRow("Star Veil", "starVeil", () -> c.starVeilEnabled, v -> c.starVeilEnabled = v, () -> c.starVeilParticle, v -> c.starVeilParticle = v));
				rows.add(particleCosmeticRow("Radiant Pulse", "radiantPulse", () -> c.radiantPulseEnabled, v -> c.radiantPulseEnabled = v, () -> c.radiantPulseParticle, v -> c.radiantPulseParticle = v));
				rows.add(colorCosmeticRow("Pulsing Sphere", "pulsingSphere", () -> c.pulsingSphereEnabled, v -> c.pulsingSphereEnabled = v, () -> c.pulsingSphereColor, v -> c.pulsingSphereColor = v, () -> c.pulsingSphereParticleKind, v -> c.pulsingSphereParticleKind = v));
				rows.add(colorCosmeticRow("Scanner", "scanner", () -> c.scannerEnabled, v -> c.scannerEnabled = v, () -> c.scannerColor, v -> c.scannerColor = v, () -> c.scannerParticleKind, v -> c.scannerParticleKind = v));
				rows.add(colorCosmeticRow("Physics Cape", "physicsCape", () -> c.physicsCapeEnabled, v -> c.physicsCapeEnabled = v, () -> c.physicsCapeColor, v -> c.physicsCapeColor = v, () -> c.physicsCapeParticleKind, v -> c.physicsCapeParticleKind = v));
				rows.add(colorCosmeticRow("Cloak", "cloak", () -> c.cloakEnabled, v -> c.cloakEnabled = v, () -> c.cloakColor, v -> c.cloakColor = v, () -> c.cloakParticleKind, v -> c.cloakParticleKind = v));
			}
			case CLOUD -> {
				rows.add(infoRow("Sync HUD positions and cosmetics to sky.melloo.me."));
				rows.add(infoRow("Requires a linked account - see /me link."));
				rows.add(boolRow("Cloud Sync", () -> c.cloudSyncEnabled, v -> c.cloudSyncEnabled = v));
				rows.add(actionRow("Push Now", () -> CloudSyncManager.push(Minecraft.getInstance())));
				rows.add(actionRow("Pull Now", () -> CloudSyncManager.forcePull(Minecraft.getInstance(), this::refreshAfterChildClosed)));
			}
		}
		rebuildRows();
	}

	private void rebuildRows() {
		clearWidgets();

		int px = panelX();
		int pw = panelWidth();
		Tab[] tabs = Tab.values();
		int tabWidth = (pw - 20) / tabs.length;
		for (int i = 0; i < tabs.length; i++) {
			Tab tab = tabs[i];
			addRenderableWidget(new TabButtonWidget(px + 10 + i * tabWidth, tabBarY(), tabWidth, TAB_HEIGHT, tab));
		}

		int listWidth = pw - 20;
		int listX = px + 10;
		int y = listTop() - scrollOffset;
		for (RowFactory row : rows) {
			if (y + ROW_H >= listTop() && y <= listBottom()) {
				addRenderableWidget(row.create(listX, y, listWidth, ROW_H));
			}
			y += ROW_H + ROW_GAP;
		}
		addRenderableWidget(new StyledButton(panelX() + (panelWidth() - 80) / 2, panelY() + panelHeight() - 28, 80, 20, "Done", 0xFF55FF55, () -> {
			EssentialsConfig.save();
			Minecraft.getInstance().setScreen(parent);
		}));

		Runnable opener = openSkyMellooScreen;
		if (opener != null) {
			int w = 120;
			addRenderableWidget(new StyledButton(px + pw - w - 10, panelY() + 8, w, 16, "SkyMelloo Config", 0xFFFF6EC7, () -> {
				EssentialsConfig.save();
				opener.run();
			}));
		}
	}

	private int contentHeight() {
		return rows.size() * (ROW_H + ROW_GAP);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		int maxScroll = Math.max(0, contentHeight() - (listBottom() - listTop()));
		scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (scrollY * ROW_H)));
		scrollByTab.put(currentTab, scrollOffset);
		rebuildRows();
		return true;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}

	@Override
	public void onClose() {
		EssentialsConfig.save();
		Minecraft.getInstance().setScreen(parent);
	}

	@Override
	public void removed() {
		// Fires regardless of close path (ESC via onClose, the Done button, or the SkyMelloo Config
		// button below all just call Minecraft#setScreen, which invokes this on the outgoing screen) -
		// covers essentially every real settings change with one hook.
		CloudSyncManager.push(Minecraft.getInstance());
		super.removed();
	}

	/** Called by CosmeticEditScreen when it closes, so the list redraws with any change immediately. */
	void refreshAfterChildClosed() {
		rebuildRows();
	}

	/**
	 * The vanilla default (see Screen#extractBackground) unconditionally applies a blur + dark menu
	 * background behind ANY open screen, completely separate from whatever extractRenderState draws
	 * itself - overriding this to do nothing is the only way to actually stop that, no amount of
	 * changing our own panel's color/position touches it.
	 */
	@Override
	public void extractBackground(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
		int px = panelX();
		int py = panelY();
		int pw = panelWidth();
		int ph = panelHeight();
		gg.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, BORDER_COLOR);
		gg.fill(px, py, px + pw, py + ph, PANEL_COLOR);
		gg.text(this.font, "MellooEssentials", px + 10, py + 10, 0xFFFFD700);
		super.extractRenderState(gg, mouseX, mouseY, partialTick);
	}

	// ---- row factories ----

	private RowFactory headerRow(String label) {
		return (x, y, w, h) -> new HeaderRowWidget(x, y, w, h, label);
	}

	private RowFactory infoRow(String label) {
		return (x, y, w, h) -> new InfoRowWidget(x, y, w, h, label);
	}

	private RowFactory boolRow(String label, BooleanSupplier getter, Consumer<Boolean> setter) {
		return (x, y, w, h) -> new BoolRowWidget(x, y, w, h, label, getter, setter, null);
	}

	/** A clickable label row with no toggle state of its own - just runs an action, see BulkCosmeticScreen. */
	private RowFactory actionRow(String label, Runnable action) {
		return (x, y, w, h) -> new ActionRowWidget(x, y, w, h, label, action);
	}

	/** Color-capable - also gets a particle-kind cycle (with a "Default (Color)" entry) alongside the color grid, see CosmeticEditScreen. */
	private RowFactory colorCosmeticRow(String label, String effectKey, BooleanSupplier getter, Consumer<Boolean> setter, java.util.function.Supplier<java.awt.Color> colorGetter, Consumer<java.awt.Color> colorSetter, java.util.function.Supplier<String> particleGetter, Consumer<String> particleSetter) {
		return (x, y, w, h) -> new BoolRowWidget(x, y, w, h, label, getter, setter,
				() -> Minecraft.getInstance().setScreen(new CosmeticEditScreen(this, label, effectKey, getter, setter, colorGetter, colorSetter, particleGetter, particleSetter)));
	}

	/** No color, but its "default" look is a special fixed combo rather than one named kind, so it still gets a "Default (Original)" entry - see CosmeticEditScreen. */
	private RowFactory defaultableParticleCosmeticRow(String label, String effectKey, BooleanSupplier getter, Consumer<Boolean> setter, java.util.function.Supplier<String> particleGetter, Consumer<String> particleSetter) {
		return (x, y, w, h) -> new BoolRowWidget(x, y, w, h, label, getter, setter,
				() -> Minecraft.getInstance().setScreen(new CosmeticEditScreen(this, label, effectKey, getter, setter, null, null, particleGetter, particleSetter, true)));
	}

	private RowFactory particleCosmeticRow(String label, String effectKey, BooleanSupplier getter, Consumer<Boolean> setter, java.util.function.Supplier<String> particleGetter, Consumer<String> particleSetter) {
		return (x, y, w, h) -> new BoolRowWidget(x, y, w, h, label, getter, setter,
				() -> Minecraft.getInstance().setScreen(new CosmeticEditScreen(this, label, effectKey, getter, setter, null, null, particleGetter, particleSetter)));
	}

	// ---- row/tab widgets ----

	private final class TabButtonWidget extends AbstractWidget {
		private final Tab tab;

		TabButtonWidget(int x, int y, int w, int h, Tab tab) {
			super(x, y, w, h, Component.literal(tab.label));
			this.tab = tab;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
			boolean active = tab == currentTab;
			int x1 = getX();
			int y1 = getY();
			int x2 = getX() + getWidth();
			int y2 = getY() + getHeight();
			gg.fill(x1, y1, x2, y2, active ? 0x4066DDFF : (this.isHovered() ? ROW_BG_HOVER : 0x20000000));
			gg.fill(x1, y2 - 2, x2, y2, active ? 0xFF66DDFF : 0x00000000);
			var font = Minecraft.getInstance().font;
			int textWidth = font.width(tab.label);
			gg.text(font, tab.label, x1 + (getWidth() - textWidth) / 2, y1 + (getHeight() - 8) / 2, active ? TEXT_ON : TEXT_OFF);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			this.defaultButtonNarrationText(output);
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			if (currentTab != tab) {
				scrollByTab.put(currentTab, scrollOffset);
				currentTab = tab;
				scrollOffset = scrollByTab.getOrDefault(tab, 0);
				buildRowsForCurrentTab();
			}
		}
	}

	private final class HeaderRowWidget extends AbstractWidget {
		private final String label;

		HeaderRowWidget(int x, int y, int w, int h, String label) {
			super(x, y, w, h, Component.literal(label));
			this.label = label;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
			int x1 = getX();
			int y2 = getY() + getHeight();
			gg.text(Minecraft.getInstance().font, label.toUpperCase(Locale.ROOT), x1 + 2, y2 - 9, 0xFF66DDFF);
			gg.fill(x1 + 2, y2 - 1, getX() + getWidth(), y2, 0x4066DDFF);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			this.defaultButtonNarrationText(output);
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			// Not interactive - just a label.
		}
	}

	private final class InfoRowWidget extends AbstractWidget {
		private final String label;

		InfoRowWidget(int x, int y, int w, int h, String label) {
			super(x, y, w, h, Component.literal(label));
			this.label = label;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
			gg.text(Minecraft.getInstance().font, label, getX() + 2, getY() + (getHeight() - 8) / 2, TEXT_OFF);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			this.defaultButtonNarrationText(output);
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			// Not interactive - just a label.
		}
	}

	/** A clickable label row with a "›" hint arrow, same look as a BoolRowWidget minus the toggle dot - just runs an action on click. */
	private final class ActionRowWidget extends AbstractWidget {
		private final String label;
		private final Runnable action;

		ActionRowWidget(int x, int y, int w, int h, String label, Runnable action) {
			super(x, y, w, h, Component.literal(label));
			this.label = label;
			this.action = action;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
			int x1 = getX();
			int y1 = getY();
			int x2 = getX() + getWidth();
			if (this.isHovered()) {
				gg.fill(x1, y1, x2, y1 + getHeight(), ROW_BG_HOVER);
			}
			gg.text(Minecraft.getInstance().font, label, x1 + 2, y1 + (getHeight() - 8) / 2, TEXT_ON);
			String arrow = "›";
			int arrowWidth = Minecraft.getInstance().font.width(arrow);
			gg.text(Minecraft.getInstance().font, arrow, x2 - arrowWidth - 4, y1 + (getHeight() - 8) / 2, TEXT_OFF);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			this.defaultButtonNarrationText(output);
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			action.run();
		}
	}

	/**
	 * One row: toggle dot + name. Left-click opens the edit popup if {@code openEditor} is non-null
	 * (this cosmetic actually has a color or particle choice) - the popup itself has the enabled
	 * toggle. Right-click ALWAYS toggles enabled/disabled directly, no matter what left-click does,
	 * for a quick on/off without opening anything.
	 */
	private final class BoolRowWidget extends AbstractWidget {
		private final String label;
		private final BooleanSupplier getter;
		private final Consumer<Boolean> setter;
		private final Runnable openEditor;

		BoolRowWidget(int x, int y, int w, int h, String label, BooleanSupplier getter, Consumer<Boolean> setter, Runnable openEditor) {
			super(x, y, w, h, Component.literal(label));
			this.label = label;
			this.getter = getter;
			this.setter = setter;
			this.openEditor = openEditor;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
			boolean enabled = getter.getAsBoolean();
			int x1 = getX();
			int y1 = getY();
			int x2 = getX() + getWidth();
			int y2 = getY() + getHeight();
			if (this.isHovered()) {
				gg.fill(x1, y1, x2, y2, ROW_BG_HOVER);
			}
			int dotSize = 6;
			int dotY = y1 + (getHeight() - dotSize) / 2;
			gg.fill(x1 + 2, dotY, x1 + 2 + dotSize, dotY + dotSize, enabled ? 0xFF55FF55 : 0xFF555555);
			gg.text(Minecraft.getInstance().font, label, x1 + 2 + dotSize + 6, y1 + (getHeight() - 8) / 2, enabled ? TEXT_ON : TEXT_OFF);
			if (openEditor != null) {
				String arrow = "›";
				int arrowWidth = Minecraft.getInstance().font.width(arrow);
				gg.text(Minecraft.getInstance().font, arrow, x2 - arrowWidth - 4, y1 + (getHeight() - 8) / 2, TEXT_OFF);
			}
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			this.defaultButtonNarrationText(output);
		}

		@Override
		protected boolean isValidClickButton(net.minecraft.client.input.MouseButtonInfo buttonInfo) {
			return buttonInfo.button() == 0 || buttonInfo.button() == 1;
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			if (event.button() == 1) {
				setter.accept(!getter.getAsBoolean());
				return;
			}
			if (openEditor != null) {
				openEditor.run();
			} else {
				setter.accept(!getter.getAsBoolean());
			}
		}
	}

	/** Same flat accent-tinted button style as SkyMelloo's StringInputScreen popup. */
	static final class StyledButton extends AbstractWidget {
		private final int accentColor;
		private final Runnable onClick;

		StyledButton(int x, int y, int width, int height, String label, int accentColor, Runnable onClick) {
			super(x, y, width, height, Component.literal(label));
			this.accentColor = accentColor;
			this.onClick = onClick;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
			int x1 = getX();
			int y1 = getY();
			int x2 = getX() + getWidth();
			int y2 = getY() + getHeight();
			int fill = (accentColor & 0x00FFFFFF) | (this.isHovered() ? 0x66000000 : 0x33000000);
			gg.fill(x1, y1, x2, y2, fill);
			gg.fill(x1, y1, x2, y1 + 1, accentColor);
			gg.fill(x1, y2 - 1, x2, y2, accentColor);
			gg.fill(x1, y1, x1 + 1, y2, accentColor);
			gg.fill(x2 - 1, y1, x2, y2, accentColor);

			var font = Minecraft.getInstance().font;
			String label = this.getMessage().getString();
			int textWidth = font.width(label);
			gg.text(font, label, x1 + (getWidth() - textWidth) / 2, y1 + (getHeight() - 8) / 2, 0xFFFFFFFF);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			this.defaultButtonNarrationText(output);
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			onClick.run();
		}
	}
}
