package com.melloo.mellooessentials.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the vanilla rotating panorama on the main menu with a single static image, stretched to
 * fill the screen - a cosmetic branding touch, not a settings-gated feature (same "always on"
 * treatment as the party/staff glow, nothing here needs to be toggleable).
 */
@Mixin(TitleScreen.class)
public abstract class TitleBackgroundMixin {
	private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath("mellooessentials", "textures/gui/title_background.png");

	@Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
	private void mellooessentials$staticBackground(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		gg.blit(BACKGROUND, 0, 0, gg.guiWidth(), gg.guiHeight(), 0.0F, 0.0F, 1.0F, 1.0F);
		ci.cancel();
	}
}
