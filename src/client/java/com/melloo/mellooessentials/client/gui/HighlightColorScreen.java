package com.melloo.mellooessentials.client.gui;

import com.melloo.mellooessentials.client.config.EssentialsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Small edit popup for Friend Highlighting - enabled toggle, glow-outline toggle, and a color grid
 * (the same 16-standard-Minecraft-color palette {@link CosmeticEditScreen} uses, copied rather than
 * shared since this has no particle-kind concept at all - friend highlighting is a plain glow color,
 * not a cosmetic effect).
 */
public class HighlightColorScreen extends Screen {
	private static final int PANEL_WIDTH = 300;
	private static final int PANEL_HEIGHT = 150;
	private static final int BORDER_COLOR = 0xFF66DDFF;
	private static final int PANEL_COLOR = 0x99101018; // translucent, matches SettingsScreen - the game stays visible
	private static final int[] MC_COLORS = {
			0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA, 0xFFAA0000, 0xFFAA00AA, 0xFFFFAA00, 0xFFAAAAAA,
			0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF, 0xFFFF5555, 0xFFFF55FF, 0xFFFFFF55, 0xFFFFFFFF
	};

	private final SettingsScreen parent;
	private final String label;
	private final BooleanSupplier enabledGetter;
	private final Consumer<Boolean> enabledSetter;
	private final BooleanSupplier outlineGetter;
	private final Consumer<Boolean> outlineSetter;
	private final Supplier<Color> colorGetter;
	private final Consumer<Color> colorSetter;

	public HighlightColorScreen(SettingsScreen parent, String label,
			BooleanSupplier enabledGetter, Consumer<Boolean> enabledSetter,
			BooleanSupplier outlineGetter, Consumer<Boolean> outlineSetter,
			Supplier<Color> colorGetter, Consumer<Color> colorSetter) {
		super(Component.literal(label));
		this.parent = parent;
		this.label = label;
		this.enabledGetter = enabledGetter;
		this.enabledSetter = enabledSetter;
		this.outlineGetter = outlineGetter;
		this.outlineSetter = outlineSetter;
		this.colorGetter = colorGetter;
		this.colorSetter = colorSetter;
	}

	private int panelX() {
		return 20;
	}

	private int panelY() {
		return (this.height - PANEL_HEIGHT) / 2;
	}

	@Override
	protected void init() {
		int px = panelX();
		int py = panelY();
		addRenderableWidget(new ToggleRowWidget(px + 12, py + 30, PANEL_WIDTH - 24, 16, "Enabled", enabledGetter, enabledSetter));
		addRenderableWidget(new ToggleRowWidget(px + 12, py + 48, PANEL_WIDTH - 24, 16, "Glow Outline (visible through walls)", outlineGetter, outlineSetter));

		int swatchSize = 24;
		int gap = 4;
		int gridY = py + 72;
		for (int i = 0; i < MC_COLORS.length; i++) {
			int col = i % 8;
			int row = i / 8;
			int sx = px + 12 + col * (swatchSize + gap);
			int sy = gridY + row * (swatchSize + gap);
			addRenderableWidget(new SwatchWidget(sx, sy, swatchSize, swatchSize, MC_COLORS[i]));
		}

		addRenderableWidget(new SettingsScreen.StyledButton(px + (PANEL_WIDTH - 80) / 2, py + PANEL_HEIGHT - 26, 80, 20, "Close", 0xFF55FF55, () -> {
			EssentialsConfig.save();
			Minecraft.getInstance().setScreen(parent);
			parent.refreshAfterChildClosed();
		}));
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
		gg.fill(px - 2, py - 2, px + PANEL_WIDTH + 2, py + PANEL_HEIGHT + 2, BORDER_COLOR);
		gg.fill(px, py, px + PANEL_WIDTH, py + PANEL_HEIGHT, PANEL_COLOR);
		gg.text(this.font, label, px + 12, py + 10, 0xFFFFD700);
		super.extractRenderState(gg, mouseX, mouseY, partialTick);
	}

	private final class ToggleRowWidget extends AbstractWidget {
		private final String text;
		private final BooleanSupplier getter;
		private final Consumer<Boolean> setter;

		ToggleRowWidget(int x, int y, int w, int h, String text, BooleanSupplier getter, Consumer<Boolean> setter) {
			super(x, y, w, h, Component.literal(text));
			this.text = text;
			this.getter = getter;
			this.setter = setter;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
			boolean enabled = getter.getAsBoolean();
			int dotSize = 6;
			int dotY = getY() + (getHeight() - dotSize) / 2;
			gg.fill(getX(), dotY, getX() + dotSize, dotY + dotSize, enabled ? 0xFF55FF55 : 0xFF555555);
			gg.text(Minecraft.getInstance().font, text, getX() + dotSize + 6, getY() + (getHeight() - 8) / 2, enabled ? 0xFFFFFFFF : 0xFFAAAAAA);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			this.defaultButtonNarrationText(output);
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			setter.accept(!getter.getAsBoolean());
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
}
