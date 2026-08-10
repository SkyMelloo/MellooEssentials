package com.melloo.mellooessentials.client.gui;

import com.melloo.mellooessentials.client.config.EssentialsConfig;
import com.melloo.mellooessentials.client.util.CloudSyncManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.IntSupplier;

/**
 * Lightweight HUD layout editor (opened via J, like Lunar Client's own HUD editor) - drag on-screen
 * HUD elements to reposition them. Draws a placeholder box for each element (sized to match its real
 * render) rather than the real HudElement, so an element that's currently hidden can still be
 * repositioned. Moved here from SkyMelloo - this mod owns the keybind unconditionally now (this is
 * the single HUD layout surface for both mods, same "always bind, don't defer" pattern already used
 * for the G/H keys), natively handling only the two elements THIS mod actually renders (Connection
 * Status, Player Info). SkyMelloo (which depends on this mod, never the other way around) registers
 * its own extra elements via {@link #setExtraElementsProvider} at startup rather than this mod
 * needing to know SkyMelloo exists at all - the same extension-point shape already used for
 * {@code SettingsScreen#setSkyMellooScreenOpener}/{@code ConnectionStatusHud#setAdminBadgeSupplier}.
 */
public class HudLayoutEditorScreen extends Screen {
	/** A single draggable HUD-position box. Public (not the private inner class it used to be as SkyMelloo's own screen) so an extra-elements provider registered from outside this class can build its own. */
	public static final class Draggable {
		final String label;
		final IntSupplier getX;
		final IntSupplier getY;
		final BiConsumer<Integer, Integer> setPos;
		final int width;
		final int height;

		public Draggable(String label, IntSupplier getX, IntSupplier getY, BiConsumer<Integer, Integer> setPos, int width, int height) {
			this.label = label;
			this.getX = getX;
			this.getY = getY;
			this.setPos = setPos;
			this.width = width;
			this.height = height;
		}
	}

	private static final int SNAP_THRESHOLD = 6;
	private static final int CORNER_TICK_LENGTH = 10;
	private static final int CORNER_TICK_COLOR = 0xFFFFAA00;

	// (screenWidth, screenHeight) -> that installation's own extra Draggables - SkyMelloo's own HUD
	// elements (Fishing Combo, Party, Dungeon Score, etc.) when it's installed, null/absent otherwise.
	private static volatile BiFunction<Integer, Integer, List<Draggable>> extraElementsProvider = null;
	// Run alongside this mod's own EssentialsConfig.save() on close/release - lets SkyMelloo persist
	// its own config (SkyMellooConfig) without this mod needing to reference that class at all.
	private static volatile Runnable extraSaveHandler = null;

	public static void setExtraElementsProvider(BiFunction<Integer, Integer, List<Draggable>> provider) {
		extraElementsProvider = provider;
	}

	public static void setExtraSaveHandler(Runnable handler) {
		extraSaveHandler = handler;
	}

	private List<Draggable> elements;
	private Draggable dragging;
	private int dragOffsetX, dragOffsetY;
	private Integer snapLineX;
	private Integer snapLineY;
	/** Corners (TOP_LEFT etc.) where the dragged box is currently equidistant from its two nearby screen edges - drawn as a small accent bracket instead of a full guide line, since it's about two PERPENDICULAR margins matching rather than an alignment target. */
	private final List<Corner> equalMarginCorners = new ArrayList<>();

	private enum Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

	public HudLayoutEditorScreen() {
		super(Component.literal("HUD Layout"));
	}

	@Override
	protected void init() {
		EssentialsConfig config = EssentialsConfig.get();
		elements = new ArrayList<>();
		// Fixed 2-line layout now (headline + one combined detail line, see
		// ConnectionStatusHud#extractRenderState's own doc comment) - always exactly 26px tall
		// regardless of connection state.
		int statusWidth = Math.max(
				this.font.width("Connected ★"),
				this.font.width("sky.melloo.me · 1h 05m 30s · 999ms")
		) + 20;
		elements.add(new Draggable(
				"Connection Status",
				() -> config.hudConnectionStatusX >= 0 ? config.hudConnectionStatusX : 6,
				() -> config.hudConnectionStatusY >= 0 ? config.hudConnectionStatusY : 6,
				(x, y) -> {
					config.hudConnectionStatusX = x;
					config.hudConnectionStatusY = y;
				},
				statusWidth, 26
		));

		// Built from the exact same lines the real HUD renders right now (see PlayerInfoHud#buildLines)
		// instead of guessing from hardcoded sample text - two fixed samples were routinely WIDER than
		// what actually renders for a given player's real FPS/ping/server/area, which meant centering
		// the (too-wide) editor box left the real, narrower HUD off-center the moment the editor closed.
		List<String> playerInfoLines = PlayerInfoHud.buildLines(Minecraft.getInstance());
		int playerInfoWidth = 8;
		for (String line : playerInfoLines) {
			playerInfoWidth = Math.max(playerInfoWidth, this.font.width(line) + 8);
		}
		int playerInfoHeight = 4 + Math.max(1, playerInfoLines.size()) * 10 + 2;
		elements.add(new Draggable(
				"Player Info",
				() -> config.hudPlayerInfoX >= 0 ? config.hudPlayerInfoX : 6,
				() -> config.hudPlayerInfoY >= 0 ? config.hudPlayerInfoY : 6,
				(x, y) -> {
					config.hudPlayerInfoX = x;
					config.hudPlayerInfoY = y;
				},
				playerInfoWidth, playerInfoHeight
		));

		BiFunction<Integer, Integer, List<Draggable>> extra = extraElementsProvider;
		if (extra != null) {
			List<Draggable> extraElements = extra.apply(this.width, this.height);
			if (extraElements != null) {
				elements.addAll(extraElements);
			}
		}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}

	@Override
	public void onClose() {
		EssentialsConfig.save();
		Runnable extraSave = extraSaveHandler;
		if (extraSave != null) {
			extraSave.run();
		}
		CloudSyncManager.push(Minecraft.getInstance());
		super.onClose();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int mx = (int) event.x();
		int my = (int) event.y();
		for (Draggable d : elements) {
			int x = d.getX.getAsInt();
			int y = d.getY.getAsInt();
			if (mx >= x - 4 && mx <= x + d.width - 4 && my >= y - 3 && my <= y + d.height - 3) {
				dragging = d;
				dragOffsetX = mx - x;
				dragOffsetY = my - y;
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (dragging == null) {
			return super.mouseDragged(event, dragX, dragY);
		}
		int rawX = (int) event.x() - dragOffsetX;
		int rawY = (int) event.y() - dragOffsetY;
		// Clamped first so you can never drag a HUD element fully off-screen.
		int clampedX = Math.max(0, Math.min(rawX, this.width - dragging.width));
		int clampedY = Math.max(0, Math.min(rawY, this.height - dragging.height));

		// Three kinds of snap target per axis - the box's LOW edge (left/top), HIGH edge
		// (right/bottom), or center can each align to a target; whichever of the three is closest
		// within threshold wins. Not just centers anymore: an element can now snap its actual edge
		// flush against the screen edge or another element's edge, not only line up by middle.
		List<Integer> lowX = new ArrayList<>(List.of(0));
		List<Integer> highX = new ArrayList<>(List.of(this.width));
		List<Integer> centerX = new ArrayList<>(List.of(this.width / 2));
		List<Integer> lowY = new ArrayList<>(List.of(0));
		List<Integer> highY = new ArrayList<>(List.of(this.height));
		List<Integer> centerY = new ArrayList<>(List.of(this.height / 2));
		for (Draggable other : elements) {
			if (other == dragging) {
				continue;
			}
			int ox = other.getX.getAsInt();
			int oy = other.getY.getAsInt();
			lowX.add(ox);
			highX.add(ox + other.width);
			centerX.add(ox + other.width / 2);
			lowY.add(oy);
			highY.add(oy + other.height);
			centerY.add(oy + other.height / 2);
		}

		int[] snappedX = snapAxis(clampedX, dragging.width, lowX, highX, centerX);
		int[] snappedY = snapAxis(clampedY, dragging.height, lowY, highY, centerY);
		snapLineX = snappedX[1] >= 0 ? snappedX[1] : null;
		snapLineY = snappedY[1] >= 0 ? snappedY[1] : null;

		int newX = snappedX[0];
		int newY = snappedY[0];
		dragging.setPos.accept(newX, newY);

		equalMarginCorners.clear();
		int marginLeft = newX;
		int marginRight = this.width - (newX + dragging.width);
		int marginTop = newY;
		int marginBottom = this.height - (newY + dragging.height);
		if (Math.abs(marginLeft - marginTop) <= SNAP_THRESHOLD) {
			equalMarginCorners.add(Corner.TOP_LEFT);
		}
		if (Math.abs(marginRight - marginTop) <= SNAP_THRESHOLD) {
			equalMarginCorners.add(Corner.TOP_RIGHT);
		}
		if (Math.abs(marginLeft - marginBottom) <= SNAP_THRESHOLD) {
			equalMarginCorners.add(Corner.BOTTOM_LEFT);
		}
		if (Math.abs(marginRight - marginBottom) <= SNAP_THRESHOLD) {
			equalMarginCorners.add(Corner.BOTTOM_RIGHT);
		}
		return true;
	}

	/**
	 * @return {new low-coordinate (x or y) for this axis, guide line position or -1 if nothing snapped}
	 * Checks the box's low edge, high edge, and center against their respective target lists and
	 * takes whichever single alignment (across all three) is closest within {@link #SNAP_THRESHOLD}.
	 */
	private static int[] snapAxis(int rawLow, int size, List<Integer> lowTargets, List<Integer> highTargets, List<Integer> centerTargets) {
		int bestLow = rawLow;
		int bestDist = SNAP_THRESHOLD + 1;
		int guideLine = -1;

		for (int target : lowTargets) {
			int dist = Math.abs(rawLow - target);
			if (dist <= SNAP_THRESHOLD && dist < bestDist) {
				bestDist = dist;
				bestLow = target;
				guideLine = target;
			}
		}
		int rawHigh = rawLow + size;
		for (int target : highTargets) {
			int dist = Math.abs(rawHigh - target);
			if (dist <= SNAP_THRESHOLD && dist < bestDist) {
				bestDist = dist;
				bestLow = target - size;
				guideLine = target;
			}
		}
		int rawCenter = rawLow + size / 2;
		for (int target : centerTargets) {
			int dist = Math.abs(rawCenter - target);
			if (dist <= SNAP_THRESHOLD && dist < bestDist) {
				bestDist = dist;
				bestLow = target - size / 2;
				guideLine = target;
			}
		}
		return new int[] { bestLow, guideLine };
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging != null) {
			dragging = null;
			snapLineX = null;
			snapLineY = null;
			equalMarginCorners.clear();
			EssentialsConfig.save();
			Runnable extraSave = extraSaveHandler;
			if (extraSave != null) {
				extraSave.run();
			}
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
		gg.centeredText(this.font, "HUD Layout — drag to move, Esc to close", this.width / 2, 10, 0xFFFFFFFF);
		if (snapLineX != null) {
			gg.fill(snapLineX, 0, snapLineX + 1, this.height, 0xAAFF6EC7);
		}
		if (snapLineY != null) {
			gg.fill(0, snapLineY, this.width, snapLineY + 1, 0xAAFF6EC7);
		}
		if (dragging != null) {
			int dx = dragging.getX.getAsInt();
			int dy = dragging.getY.getAsInt();
			for (Corner corner : equalMarginCorners) {
				drawCornerTick(gg, corner, dx, dy, dragging.width, dragging.height);
			}
		}
		for (Draggable d : elements) {
			int x = d.getX.getAsInt();
			int y = d.getY.getAsInt();
			boolean active = d == dragging;
			gg.fill(x - 4, y - 3, x + d.width - 4, y + d.height - 3, active ? 0xAAFF6EC7 : 0x6633CC66);
			gg.outline(x - 4, y - 3, d.width, d.height, 0xFFFFFFFF);
			gg.centeredText(this.font, d.label, x + (d.width - 8) / 2, y + (d.height - 8) / 2 - 3, 0xFFFFFFFF);
		}
		super.extractRenderState(gg, mouseX, mouseY, partialTick);
	}

	/** A short L-shaped bracket at one corner of the dragged box, indicating its margins to the two nearby screen edges are currently equal - a different kind of alignment than the full-line edge/center guides above, since it's about two PERPENDICULAR gaps matching rather than a shared position. */
	private void drawCornerTick(GuiGraphicsExtractor gg, Corner corner, int x, int y, int width, int height) {
		int left = x, right = x + width, top = y, bottom = y + height;
		switch (corner) {
			case TOP_LEFT -> {
				gg.fill(left, top, left + CORNER_TICK_LENGTH, top + 1, CORNER_TICK_COLOR);
				gg.fill(left, top, left + 1, top + CORNER_TICK_LENGTH, CORNER_TICK_COLOR);
			}
			case TOP_RIGHT -> {
				gg.fill(right - CORNER_TICK_LENGTH, top, right, top + 1, CORNER_TICK_COLOR);
				gg.fill(right - 1, top, right, top + CORNER_TICK_LENGTH, CORNER_TICK_COLOR);
			}
			case BOTTOM_LEFT -> {
				gg.fill(left, bottom - 1, left + CORNER_TICK_LENGTH, bottom, CORNER_TICK_COLOR);
				gg.fill(left, bottom - CORNER_TICK_LENGTH, left + 1, bottom, CORNER_TICK_COLOR);
			}
			case BOTTOM_RIGHT -> {
				gg.fill(right - CORNER_TICK_LENGTH, bottom - 1, right, bottom, CORNER_TICK_COLOR);
				gg.fill(right - 1, bottom - CORNER_TICK_LENGTH, right, bottom, CORNER_TICK_COLOR);
			}
		}
	}
}
