package com.melloo.mellooessentials.client.mixin;

import com.melloo.mellooessentials.client.highlight.HighlightManager;
import com.melloo.mellooessentials.client.social.ModMarkerManager;
import com.melloo.mellooessentials.client.social.PresenceManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Targets BOTH Entity and Player: Player.getDisplayName() is a full override (calls
 * decorateDisplayNameComponent, not Entity's default team-formatting logic), so a mixin on
 * Entity.class alone would silently never run for Player instances at all.
 * <p>
 * The only place a nametag marker gets added, even when SkyMelloo (which depends on this mod) is
 * also installed - see {@link ModMarkerManager}'s own doc comment for why the pink-vs-light-blue
 * choice is resolved from server-provided presence data ({@link PresenceManager#isSkyMelloo}) here
 * directly, rather than a second mixin stacking a second icon on top.
 */
@Mixin({Entity.class, Player.class})
public abstract class EntityDisplayNameMixin {
	private static final Identifier LIGHT_BLUE_DYE_SPRITE = Identifier.withDefaultNamespace("item/light_blue_dye");
	private static final Identifier PINK_DYE_SPRITE = Identifier.withDefaultNamespace("item/pink_dye");

	@Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
	private void mellooessentials$colorizeName(CallbackInfoReturnable<Component> cir) {
		Entity self = (Entity) (Object) this;
		if (self instanceof Player player) {
			Component colorized = HighlightManager.colorizeName(player, cir.getReturnValue());
			if (ModMarkerManager.isModUser(player.getUUID())) {
				Identifier sprite = PresenceManager.isSkyMelloo(player.getUUID()) ? PINK_DYE_SPRITE : LIGHT_BLUE_DYE_SPRITE;
				colorized = ModMarkerManager.apply(player.getUUID(), colorized, sprite);
			}
			cir.setReturnValue(colorized);
		}
	}
}
