package com.melloo.mellooessentials.client.gui;

import com.melloo.mellooessentials.client.config.EssentialsConfig;
import com.melloo.mellooessentials.client.cosmetics.ParticleKind;
import com.melloo.mellooessentials.client.util.Lang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Centered per-cosmetic edit popup (same bordered-card style as SkyMelloo's StringInputScreen).
 * Every cosmetic gets an enabled toggle and a "hide my own particles" toggle. A cosmetic that's
 * color-capable ALSO gets a particle-kind cycle whose first entry is "Default (Color)" - picking it
 * shows the 16-standard-Minecraft-color grid below and renders as colored dust; picking any named
 * kind (Heart, Note, Flame, ...) hides the color grid, since a named vanilla particle has its own
 * fixed, non-recolorable look - the two are mutually exclusive at the game-particle-system level,
 * not an arbitrary limitation here. A cosmetic that was never color-capable to begin with (no
 * colorGetter) just cycles the named kinds directly, with no "Default" entry.
 */
public class CosmeticEditScreen extends Screen {
	private static final int PANEL_WIDTH = 300;
	private static final int BORDER_COLOR = 0xFF66DDFF;
	private static final int PANEL_COLOR = 0x99101018; // translucent, matches SettingsScreen - the game stays visible
	private static final int[] MC_COLORS = {
			0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA, 0xFFAA0000, 0xFFAA00AA, 0xFFFFAA00, 0xFFAAAAAA,
			0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF, 0xFFFF5555, 0xFFFF55FF, 0xFFFFFF55, 0xFFFFFFFF
	};

	private final SettingsScreen parent;
	private final String label;
	private final String effectKey;
	private final BooleanSupplier enabledGetter;
	private final Consumer<Boolean> enabledSetter;
	private final Supplier<Color> colorGetter;
	private final Consumer<Color> colorSetter;
	private final Supplier<String> particleGetter;
	private final Consumer<String> particleSetter;
	private final boolean particleHasDefault;

	public CosmeticEditScreen(SettingsScreen parent, String label, String effectKey, BooleanSupplier enabledGetter, Consumer<Boolean> enabledSetter,
			Supplier<Color> colorGetter, Consumer<Color> colorSetter, Supplier<String> particleGetter, Consumer<String> particleSetter) {
		this(parent, label, effectKey, enabledGetter, enabledSetter, colorGetter, colorSetter, particleGetter, particleSetter, colorGetter != null);
	}

	/**
	 * @param particleHasDefault whether the particle cycle should include a "Default" entry (null)
	 *                           representing this cosmetic's own original look - always true when
	 *                           {@code colorGetter} is non-null (Default = colored dust), but also
	 *                           true for a few particle-only cosmetics whose default is a special
	 *                           fixed combo (Rain Cloud's cloud+rain, Campfire Smoke's smoke mix,
	 *                           Confetti Burst's rainbow) rather than a single named kind.
	 */
	public CosmeticEditScreen(SettingsScreen parent, String label, String effectKey, BooleanSupplier enabledGetter, Consumer<Boolean> enabledSetter,
			Supplier<Color> colorGetter, Consumer<Color> colorSetter, Supplier<String> particleGetter, Consumer<String> particleSetter, boolean particleHasDefault) {
		super(Component.literal(label));
		this.parent = parent;
		this.label = label;
		this.effectKey = effectKey;
		this.enabledGetter = enabledGetter;
		this.enabledSetter = enabledSetter;
		this.colorGetter = colorGetter;
		this.colorSetter = colorSetter;
		this.particleGetter = particleGetter;
		this.particleSetter = particleSetter;
		this.particleHasDefault = particleHasDefault;
	}

	/** Whether the color grid should currently be showing - only for color-capable cosmetics still on "Default (Color)". */
	private boolean showColorGrid() {
		return colorGetter != null && particleGetter.get() == null;
	}

	private int panelHeight() {
		if (colorGetter != null) {
			return showColorGrid() ? 190 : 128;
		}
		if (particleGetter != null) {
			return 110;
		}
		return 90;
	}

	private static final int PANEL_LEFT_MARGIN = 20;

	private int panelX() {
		return PANEL_LEFT_MARGIN;
	}

	private int panelY() {
		return (this.height - panelHeight()) / 2;
	}

	@Override
	protected void init() {
		int px = panelX();
		int py = panelY();
		addRenderableWidget(new ToggleRowWidget(px + 12, py + 30, PANEL_WIDTH - 24, 16));
		addRenderableWidget(new HideSelfRowWidget(px + 12, py + 48, PANEL_WIDTH - 24, 16));

		if (particleGetter != null) {
			addRenderableWidget(new ParticleCycleWidget(px + 12, py + 68, PANEL_WIDTH - 24, 20, particleHasDefault));
		}

		if (showColorGrid()) {
			int swatchSize = 24;
			int gap = 4;
			int gridY = py + 92;
			for (int i = 0; i < MC_COLORS.length; i++) {
				int col = i % 8;
				int row = i / 8;
				int sx = px + 12 + col * (swatchSize + gap);
				int sy = gridY + row * (swatchSize + gap);
				int color = MC_COLORS[i];
				addRenderableWidget(new SwatchWidget(sx, sy, swatchSize, swatchSize, color));
			}
		}

		addRenderableWidget(new SettingsScreen.StyledButton(px + (PANEL_WIDTH - 80) / 2, py + panelHeight() - 26, 80, 20, Lang.s("mellooessentials.gui.common.close"), 0xFF55FF55, () -> {
			EssentialsConfig.save();
			Minecraft.getInstance().setScreen(parent);
			parent.refreshAfterChildClosed();
		}));
	}

	/** Reopens a fresh copy of this same screen - the simplest way to reflow the layout when picking a particle kind changes whether the color grid should show. */
	private void reopenSelf() {
		Minecraft.getInstance().setScreen(new CosmeticEditScreen(parent, label, effectKey, enabledGetter, enabledSetter, colorGetter, colorSetter, particleGetter, particleSetter, particleHasDefault));
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}

	@Override
	public void onClose() {
		EssentialsConfig.save();
		Minecraft.getInstance().setScreen(parent);
		parent.refreshAfterChildClosed();
	}

	/** See SettingsScreen's own override of this - the vanilla default applies a blur+dark background regardless of anything drawn in extractRenderState. */
	@Override
	public void extractBackground(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
		int px = panelX();
		int py = panelY();
		int ph = panelHeight();
		gg.fill(px - 2, py - 2, px + PANEL_WIDTH + 2, py + ph + 2, BORDER_COLOR);
		gg.fill(px, py, px + PANEL_WIDTH, py + ph, PANEL_COLOR);
		gg.text(this.font, label, px + 12, py + 10, 0xFFFFD700);
		super.extractRenderState(gg, mouseX, mouseY, partialTick);
	}

	private final class ToggleRowWidget extends AbstractWidget {
		ToggleRowWidget(int x, int y, int w, int h) {
			super(x, y, w, h, Lang.c("mellooessentials.gui.common.enabled"));
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
			boolean enabled = enabledGetter.getAsBoolean();
			int dotSize = 6;
			int dotY = getY() + (getHeight() - dotSize) / 2;
			gg.fill(getX(), dotY, getX() + dotSize, dotY + dotSize, enabled ? 0xFF55FF55 : 0xFF555555);
			gg.text(Minecraft.getInstance().font, Lang.s("mellooessentials.gui.common.enabled"), getX() + dotSize + 6, getY() + (getHeight() - 8) / 2, enabled ? 0xFFFFFFFF : 0xFFAAAAAA);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			this.defaultButtonNarrationText(output);
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			enabledSetter.accept(!enabledGetter.getAsBoolean());
		}
	}

	/** Still reported to other players (they keep seeing it via presence sync) - only suppresses the local self-render call, see EssentialsConfig#isSelfHidden. */
	private final class HideSelfRowWidget extends AbstractWidget {
		HideSelfRowWidget(int x, int y, int w, int h) {
			super(x, y, w, h, Lang.c("mellooessentials.gui.cosmetic_edit.hide_own_particles"));
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
			boolean hidden = EssentialsConfig.get().isSelfHidden(effectKey);
			int dotSize = 6;
			int dotY = getY() + (getHeight() - dotSize) / 2;
			gg.fill(getX(), dotY, getX() + dotSize, dotY + dotSize, hidden ? 0xFF55FF55 : 0xFF555555);
			gg.text(Minecraft.getInstance().font, Lang.s("mellooessentials.gui.cosmetic_edit.hide_own_particles"), getX() + dotSize + 6, getY() + (getHeight() - 8) / 2, hidden ? 0xFFFFFFFF : 0xFFAAAAAA);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			this.defaultButtonNarrationText(output);
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			EssentialsConfig.get().setSelfHidden(effectKey, !EssentialsConfig.get().isSelfHidden(effectKey));
		}
	}

	private final class SwatchWidget extends AbstractWidget {
		private final int color;

		SwatchWidget(int x, int y, int w, int h, int color) {
			super(x, y, w, h, Component.literal("#" + Integer.toHexString(color)));
			this.color = color;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
			boolean selected = (colorGetter.get().getRGB() | 0xFF000000) == color;
			if (selected || this.isHovered()) {
				gg.fill(getX() - 2, getY() - 2, getX() + getWidth() + 2, getY() + getHeight() + 2, selected ? 0xFFFFFFFF : 0xFF888888);
			}
			gg.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), color);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			this.defaultButtonNarrationText(output);
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			colorSetter.accept(new Color(color, true));
		}
	}

	/**
	 * Cycles through particle kinds. When {@code hasDefault} is true (this cosmetic is also color-
	 * capable), a null-backed "Default (Color)" entry is prepended to the cycle and picking anything
	 * else reopens the screen so the color grid can appear/disappear.
	 */
	private final class ParticleCycleWidget extends AbstractWidget {
		private final boolean hasDefault;
		private final List<String> options = new ArrayList<>();

		ParticleCycleWidget(int x, int y, int w, int h, boolean hasDefault) {
			super(x, y, w, h, Lang.c("mellooessentials.gui.common.particle"));
			this.hasDefault = hasDefault;
			if (hasDefault) {
				options.add(null);
			}
			for (ParticleKind kind : ParticleKind.values()) {
				options.add(kind.name());
			}
		}

		private String currentLabel() {
			String current = particleGetter.get();
			if (current == null) {
				return colorGetter != null ? Lang.s("mellooessentials.gui.cosmetic_edit.default_color") : Lang.s("mellooessentials.gui.cosmetic_edit.default_original");
			}
			return ParticleKind.byNameOr(current, ParticleKind.values()[0]).label();
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
			if (this.isHovered()) {
				gg.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x14FFFFFF);
			}
			String text = Lang.s("mellooessentials.gui.particle_cycle.label", currentLabel());
			gg.text(Minecraft.getInstance().font, text, getX() + 2, getY() + (getHeight() - 8) / 2, 0xFFFFFFFF);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			this.defaultButtonNarrationText(output);
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			String current = particleGetter.get();
			int index = options.indexOf(current);
			String next = options.get((index + 1) % options.size());
			particleSetter.accept(next);
			if (hasDefault) {
				// Color grid visibility just changed - reflow the whole popup rather than trying to
				// patch widgets in place.
				reopenSelf();
			}
		}
	}
}
