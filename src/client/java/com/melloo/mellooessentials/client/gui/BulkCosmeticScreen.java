package com.melloo.mellooessentials.client.gui;

import com.melloo.mellooessentials.client.config.EssentialsConfig;
import com.melloo.mellooessentials.client.cosmetics.ParticleKind;
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

/**
 * A quick bulk-apply popup opened from the Cosmetics tab's master switch - picks either one color
 * or one particle kind and immediately applies it to every cosmetic that has that option at all
 * (see EssentialsConfig#setAllColors/setAllParticleKinds), instead of opening each cosmetic's own
 * edit popup one at a time.
 */
public class BulkCosmeticScreen extends Screen {
	private static final int PANEL_WIDTH = 300;
	private static final int BORDER_COLOR = 0xFF66DDFF;
	private static final int PANEL_COLOR = 0x99101018; // translucent, matches SettingsScreen - the game stays visible
	private static final int[] MC_COLORS = {
			0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA, 0xFFAA0000, 0xFFAA00AA, 0xFFFFAA00, 0xFFAAAAAA,
			0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF, 0xFFFF5555, 0xFFFF55FF, 0xFFFFFF55, 0xFFFFFFFF
	};

	private final SettingsScreen parent;
	private final boolean colorMode;
	private String selectedParticleKind = null; // local cycle state, not read from config - there's no single "current" value across every cosmetic

	public BulkCosmeticScreen(SettingsScreen parent, boolean colorMode) {
		super(Component.literal(colorMode ? "Set All Colors" : "Set All Particle Kinds"));
		this.parent = parent;
		this.colorMode = colorMode;
	}

	private int panelHeight() {
		return colorMode ? 130 : 80;
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
		if (colorMode) {
			int swatchSize = 24;
			int gap = 4;
			int gridY = py + 30;
			for (int i = 0; i < MC_COLORS.length; i++) {
				int col = i % 8;
				int row = i / 8;
				int sx = px + 12 + col * (swatchSize + gap);
				int sy = gridY + row * (swatchSize + gap);
				int color = MC_COLORS[i];
				addRenderableWidget(new SwatchWidget(sx, sy, swatchSize, swatchSize, color));
			}
		} else {
			addRenderableWidget(new ParticleCycleWidget(px + 12, py + 30, PANEL_WIDTH - 24, 20));
		}

		addRenderableWidget(new SettingsScreen.StyledButton(px + (PANEL_WIDTH - 80) / 2, py + panelHeight() - 26, 80, 20, "Close", 0xFF55FF55, this::close));
	}

	private void close() {
		EssentialsConfig.save();
		Minecraft.getInstance().setScreen(parent);
		parent.refreshAfterChildClosed();
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}

	@Override
	public void onClose() {
		close();
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
		gg.text(this.font, this.getTitle().getString(), px + 12, py + 10, 0xFFFFD700);
		super.extractRenderState(gg, mouseX, mouseY, partialTick);
	}

	private final class SwatchWidget extends AbstractWidget {
		private final int color;

		SwatchWidget(int x, int y, int w, int h, int color) {
			super(x, y, w, h, Component.literal("#" + Integer.toHexString(color)));
			this.color = color;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
			if (this.isHovered()) {
				gg.fill(getX() - 2, getY() - 2, getX() + getWidth() + 2, getY() + getHeight() + 2, 0xFF888888);
			}
			gg.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), color);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			this.defaultButtonNarrationText(output);
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			EssentialsConfig.get().setAllColors(new Color(color, true));
			EssentialsConfig.save();
		}
	}

	private final class ParticleCycleWidget extends AbstractWidget {
		private final List<String> options = new ArrayList<>();

		ParticleCycleWidget(int x, int y, int w, int h) {
			super(x, y, w, h, Component.literal("Particle"));
			options.add(null);
			for (ParticleKind kind : ParticleKind.values()) {
				options.add(kind.name());
			}
		}

		private String currentLabel() {
			if (selectedParticleKind == null) {
				return "Default (each cosmetic's own look)";
			}
			return ParticleKind.byNameOr(selectedParticleKind, ParticleKind.values()[0]).label;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
			if (this.isHovered()) {
				gg.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x14FFFFFF);
			}
			String text = "Particle: " + currentLabel() + "  (click to cycle)";
			gg.text(Minecraft.getInstance().font, text, getX() + 2, getY() + (getHeight() - 8) / 2, 0xFFFFFFFF);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			this.defaultButtonNarrationText(output);
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			int index = options.indexOf(selectedParticleKind);
			selectedParticleKind = options.get((index + 1) % options.size());
			EssentialsConfig.get().setAllParticleKinds(selectedParticleKind);
			EssentialsConfig.save();
		}
	}
}
