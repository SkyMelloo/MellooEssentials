package com.melloo.mellooessentials.client.mixin;

import com.melloo.mellooessentials.client.highlight.HighlightManager;
import com.melloo.mellooessentials.client.social.ModMarkerManager;
import com.melloo.mellooessentials.client.social.PresenceManager;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

// Forces a party/team member's tab-list row name to the fixed highlight color, and prepends the
// same mod-user marker EntityDisplayNameMixin adds to the in-world nametag.
@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
	private static final Identifier LIGHT_BLUE_DYE_SPRITE = Identifier.withDefaultNamespace("item/light_blue_dye");
	private static final Identifier PINK_DYE_SPRITE = Identifier.withDefaultNamespace("item/pink_dye");

	@Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
	private void mellooessentials$colorizeTabName(PlayerInfo playerInfo, CallbackInfoReturnable<Component> cir) {
		UUID uuid = playerInfo.getProfile().id();
		Component colorized = HighlightManager.colorizeTabListName(uuid, playerInfo.getProfile().name(), cir.getReturnValue());
		if (ModMarkerManager.isModUser(uuid)) {
			Identifier sprite = PresenceManager.isSkyMelloo(uuid) ? PINK_DYE_SPRITE : LIGHT_BLUE_DYE_SPRITE;
			colorized = ModMarkerManager.apply(uuid, colorized, sprite);
		}
		cir.setReturnValue(colorized);
	}
}
