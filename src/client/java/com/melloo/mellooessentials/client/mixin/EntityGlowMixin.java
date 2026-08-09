package com.melloo.mellooessentials.client.mixin;

import com.melloo.mellooessentials.client.highlight.HighlightManager;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forces the vanilla glow-outline (the same effect used by the Glowing status effect, which
 * already renders through walls) onto party members. Purely client-side rendering: no packets are
 * sent, no server-side entity state changes.
 */
@Mixin(Entity.class)
public abstract class EntityGlowMixin {

	@Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
	private void mellooessentials$forceGlow(CallbackInfoReturnable<Boolean> cir) {
		Entity self = (Entity) (Object) this;
		if (HighlightManager.shouldGlow(self)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
	private void mellooessentials$glowColor(CallbackInfoReturnable<Integer> cir) {
		Entity self = (Entity) (Object) this;
		if (HighlightManager.shouldGlow(self)) {
			cir.setReturnValue(HighlightManager.getGlowColor(self));
		}
	}
}
