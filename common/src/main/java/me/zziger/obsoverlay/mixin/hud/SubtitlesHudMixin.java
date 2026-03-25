package me.zziger.obsoverlay.mixin.hud;

import me.zziger.obsoverlay.OverlayRenderer;
import me.zziger.obsoverlay.registry.AllDefaultOverlayComponents;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.SubtitlesHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// priority = 1500: see ChatHudMixin for the full rationale.
// ImmediatelyFast's MixinSubtitlesHud injects beginHudBatching/endHudBatching
// at HEAD/RETURN with default priority 1000.
@Mixin(value = SubtitlesHud.class, priority = 1500)
public class SubtitlesHudMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void drawStart(DrawContext context, CallbackInfo ci) {
        OverlayRenderer.beginDraw(AllDefaultOverlayComponents.subtitles, context);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void drawEnd(DrawContext context, CallbackInfo ci) {
        OverlayRenderer.endDraw(AllDefaultOverlayComponents.subtitles, context);
    }
}
