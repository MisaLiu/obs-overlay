package me.zziger.obsoverlay.mixin.hud;

import me.zziger.obsoverlay.OverlayRenderer;
import me.zziger.obsoverlay.registry.AllDefaultOverlayComponents;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// priority = 1500: see ChatHudMixin for the full rationale.
// ImmediatelyFast's MixinPlayerListHud also injects beginHudBatching/endHudBatching
// at HEAD/RETURN with default priority 1000. We must run after it at HEAD so that
// IF's drawContext.draw() fires on the main framebuffer, and our overlay FBO switch
// happens afterwards with the batch already flushed.
@Mixin(value = PlayerListHud.class, priority = 1500)
public class PlayerListHudMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void drawStart(DrawContext context, int scaledWindowWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
        OverlayRenderer.beginDraw(AllDefaultOverlayComponents.playerList, context);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void drawEnd(DrawContext context, int scaledWindowWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
        OverlayRenderer.endDraw(AllDefaultOverlayComponents.playerList, context);
    }
}
