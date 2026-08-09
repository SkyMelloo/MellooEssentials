package com.melloo.mellooessentials.client.mixin;

import com.melloo.mellooessentials.client.highlight.HighlightManager;
import com.melloo.mellooessentials.client.social.ModMarkerManager;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Forces a party/team member's Tab-list (player list) row name to the fixed highlight color,
 * overriding whatever color Hypixel's own scoreboard-team rank formatting would otherwise show -
 * see {@link HighlightManager#colorizeTabListName} - and prepends the same mod-user marker
 * {@link EntityDisplayNameMixin} adds to the in-world nametag, so it shows up in the tab list too.
 * Keyed by {@link PlayerInfo}, not a loaded entity, since the Tab-list can show players who aren't
 * even in render distance.
 */
@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
	private static final Identifier LIGHT_BLUE_DYE_SPRITE = Identifier.withDefaultNamespace("item/light_blue_dye");

	@Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
	private void mellooessentials$colorizeTabName(PlayerInfo playerInfo, CallbackInfoReturnable<Component> cir) {
		UUID uuid = playerInfo.getProfile().id();
		Component colorized = HighlightManager.colorizeTabListName(uuid, cir.getReturnValue());
		if (ModMarkerManager.isModUser(uuid)) {
			colorized = ModMarkerManager.apply(uuid, colorized, LIGHT_BLUE_DYE_SPRITE);
		}
		cir.setReturnValue(colorized);
	}
}
