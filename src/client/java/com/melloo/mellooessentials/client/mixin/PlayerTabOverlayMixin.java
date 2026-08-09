package com.melloo.mellooessentials.client.mixin;

import com.melloo.mellooessentials.client.highlight.HighlightManager;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forces a party/team member's Tab-list (player list) row name to the fixed highlight color,
 * overriding whatever color Hypixel's own scoreboard-team rank formatting would otherwise show -
 * see {@link HighlightManager#colorizeTabListName}. Keyed by {@link PlayerInfo}, not a loaded
 * entity, since the Tab-list can show players who aren't even in render distance.
 */
@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {

	@Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
	private void mellooessentials$colorizeTabName(PlayerInfo playerInfo, CallbackInfoReturnable<Component> cir) {
		cir.setReturnValue(HighlightManager.colorizeTabListName(playerInfo.getProfile().id(), cir.getReturnValue()));
	}
}
