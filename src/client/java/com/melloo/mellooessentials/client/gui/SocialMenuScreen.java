package com.melloo.mellooessentials.client.gui;

import com.melloo.mellooessentials.client.api.ApiClient;
import com.melloo.mellooessentials.client.social.FriendsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * "Social" menu (key G) - the friends list (separate from real Hypixel friends, see
 * {@link FriendsManager}). Moved here from SkyMelloo along with the rest of the Friends system;
 * unlike the old SkyMelloo version this has no Party column - party membership/kick/block stay
 * SkyMelloo-only features (richer MP/floor data, auto-kick rules) with their own existing chat
 * commands, not duplicated into this simpler standalone-friendly screen.
 * <p>
 * Every text label's (x, y) is computed exactly once, alongside its matching button, into
 * {@link #labels} during {@link #rebuild()} - {@link #extractRenderState} then just draws from that
 * list rather than recomputing the same layout a second time. Face icons ({@link #faces}) are the
 * same idea, keyed by UUID and resolved lazily via {@link RemoteFaceTextureCache} - which works
 * over HTTP regardless of whether that player is anywhere nearby, since a friend here can be
 * offline or on a completely different part of the network.
 */
public class SocialMenuScreen extends Screen {
	private record Label(String text, int x, int y) {
	}

	private record Face(UUID uuid, int x, int y) {
	}

	private static final int ROW_HEIGHT = 26;
	private static final int COLUMN_WIDTH = 260;
	private static final int FACE_SIZE = 18;
	private static final int FACE_TEXT_GAP = 6;
	private static final int TEXT_X_OFFSET = FACE_SIZE + FACE_TEXT_GAP;
	private static final int BLUE = 0xFF66DDFF;
	private static final int GREEN = 0xFF55FF88;
	private static final int RED = 0xFFFF5555;

	private EditBox addFriendBox;
	private final List<Label> labels = new ArrayList<>();
	private final List<Face> faces = new ArrayList<>();
	private int lastFriendsHash;
	private int lastRequestsHash;

	public SocialMenuScreen() {
		super(Component.literal("Social"));
	}

	@Override
	protected void init() {
		FriendsManager.refresh(Minecraft.getInstance());
		rebuild();
	}

	@Override
	public void tick() {
		super.tick();
		// Friend requests can arrive while this screen is sitting open - re-checking every tick
		// against a cheap hash is simpler than wiring a proper change-listener into FriendsManager's
		// own independently-polling state.
		int friendsHash = FriendsManager.getFriends().hashCode();
		int requestsHash = FriendsManager.getIncomingRequests().hashCode();
		if (friendsHash != lastFriendsHash || requestsHash != lastRequestsHash) {
			rebuild();
		}
	}

	/** Lenient parse of a Mojang UUID string in either dashed or dashless form - null (no face icon) rather than throwing if it's ever something unexpected. */
	private static UUID parseUuid(String raw) {
		if (raw == null || raw.isEmpty()) {
			return null;
		}
		try {
			if (raw.length() == 32) {
				return UUID.fromString(raw.replaceFirst(
						"(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
			}
			return UUID.fromString(raw);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private void rebuild() {
		clearWidgets();
		labels.clear();
		faces.clear();
		lastFriendsHash = FriendsManager.getFriends().hashCode();
		lastRequestsHash = FriendsManager.getIncomingRequests().hashCode();

		int x = this.width / 2 - COLUMN_WIDTH / 2;
		int top = 46;

		buildFriendsColumn(x, top);

		addRenderableWidget(new SettingsScreen.StyledButton(this.width / 2 - 40, this.height - 28, 80, 20, "Done", BLUE, this::onClose));
	}

	private void buildFriendsColumn(int x, int top) {
		int y = top;
		addFriendBox = new EditBox(this.font, x, y, COLUMN_WIDTH - 60, 18, Component.literal("Add friend"));
		addFriendBox.setHint(Component.literal("Add friend..."));
		addFriendBox.setTextColor(0xFFB6E6FF);
		addRenderableWidget(addFriendBox);
		addRenderableWidget(new SettingsScreen.StyledButton(x + COLUMN_WIDTH - 55, y, 55, 18, "Add", BLUE, () -> {
			String name = addFriendBox.getValue().trim();
			if (!name.isEmpty()) {
				FriendsManager.sendRequest(Minecraft.getInstance(), name);
				addFriendBox.setValue("");
			}
		}));
		y += ROW_HEIGHT;

		List<ApiClient.FriendRequestEntry> requests = FriendsManager.getIncomingRequests();
		for (ApiClient.FriendRequestEntry request : requests) {
			UUID uuid = parseUuid(request.uuid());
			if (uuid != null) {
				faces.add(new Face(uuid, x, y));
			}
			addRenderableWidget(new SettingsScreen.StyledButton(x + COLUMN_WIDTH - 44, y, 20, 18, "✓", GREEN, () -> FriendsManager.accept(Minecraft.getInstance(), request.username())));
			addRenderableWidget(new SettingsScreen.StyledButton(x + COLUMN_WIDTH - 22, y, 20, 18, "✕", RED, () -> FriendsManager.decline(Minecraft.getInstance(), request.username())));
			labels.add(new Label("§b" + request.username() + " §7(request)", x + TEXT_X_OFFSET, y + (FACE_SIZE - 8) / 2));
			y += ROW_HEIGHT;
		}

		List<ApiClient.FriendEntry> friends = FriendsManager.getFriends();
		for (ApiClient.FriendEntry friend : friends) {
			UUID uuid = parseUuid(friend.uuid());
			if (uuid != null) {
				faces.add(new Face(uuid, x, y));
			}
			addRenderableWidget(new SettingsScreen.StyledButton(x + COLUMN_WIDTH - 100, y, 45, 18, "Chat", BLUE, () ->
					Minecraft.getInstance().setScreen(new ChatScreen("/me chat " + friend.username() + " ", true))));
			addRenderableWidget(new SettingsScreen.StyledButton(x + COLUMN_WIDTH - 52, y, 52, 18, "Remove", RED, () -> FriendsManager.remove(Minecraft.getInstance(), friend.username())));
			labels.add(new Label("§b" + friend.username(), x + TEXT_X_OFFSET, y + (FACE_SIZE - 8) / 2));
			y += ROW_HEIGHT;
		}

		if (friends.isEmpty() && requests.isEmpty()) {
			labels.add(new Label("§7No friends yet.", x, y + 5));
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
		gg.fill(0, 0, this.width, this.height, 0xCC101018);

		int x = this.width / 2 - COLUMN_WIDTH / 2;
		int top = 46;

		gg.centeredText(this.font, "§bSocial", this.width / 2, 16, 0xFFFFFF);
		gg.text(this.font, "§dFriends", x, top - 14, 0xFFFFFF);

		for (Face face : faces) {
			Identifier texture = RemoteFaceTextureCache.get(face.uuid());
			if (texture != null) {
				gg.blit(texture, face.x(), face.y(), face.x() + FACE_SIZE, face.y() + FACE_SIZE, 0f, 1f, 0f, 1f);
			} else {
				gg.fill(face.x(), face.y(), face.x() + FACE_SIZE, face.y() + FACE_SIZE, 0x30FFFFFF);
			}
		}
		for (Label label : labels) {
			gg.text(this.font, label.text(), label.x(), label.y(), 0xFFFFFF);
		}

		super.extractRenderState(gg, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
