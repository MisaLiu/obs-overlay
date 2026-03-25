package me.zziger.obsoverlay.mixin.hud;

import me.zziger.obsoverlay.OverlayRenderer;
import me.zziger.obsoverlay.registry.AllDefaultOverlayComponents;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("HEAD"))
    private void drawStartScoreboard(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        OverlayRenderer.beginDraw(AllDefaultOverlayComponents.scoreboards, context);
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("RETURN"))
    private void drawEndScoreboard(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        OverlayRenderer.endDraw(AllDefaultOverlayComponents.scoreboards, context);
    }

    @Inject(method = "renderOverlayMessage", at = @At("HEAD"))
    private void drawStartActionbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        OverlayRenderer.beginDraw(AllDefaultOverlayComponents.actionbar, context);
    }

    @Inject(method = "renderOverlayMessage", at = @At("RETURN"))
    private void drawEndActionbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        OverlayRenderer.endDraw(AllDefaultOverlayComponents.actionbar, context);
    }

    @Inject(method = "renderTitleAndSubtitle", at = @At("HEAD"))
    private void drawStartTitleSubtitle(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        OverlayRenderer.beginDraw(AllDefaultOverlayComponents.titleSubtitle, context);
    }

    @Inject(method = "renderTitleAndSubtitle", at = @At("RETURN"))
    private void drawEndTitleSubtitle(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        OverlayRenderer.endDraw(AllDefaultOverlayComponents.titleSubtitle, context);
    }

    @Inject(method = "renderExperienceLevel", at = @At("HEAD"))
    private void drawStartExperienceLevel(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        OverlayRenderer.beginDraw(AllDefaultOverlayComponents.mainHud, context);
    }

    @Inject(method = "renderExperienceLevel", at = @At("RETURN"))
    private void drawEndExperienceLevel(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        OverlayRenderer.endDraw(AllDefaultOverlayComponents.mainHud, context);
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"))
    private void drawStartEffects(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        OverlayRenderer.beginDraw(AllDefaultOverlayComponents.effects, context);
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("RETURN"))
    private void drawEndEffects(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        OverlayRenderer.endDraw(AllDefaultOverlayComponents.effects, context);
    }

    @Inject(method = "renderMainHud", at = @At("HEAD"))
    private void drawStartMainHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        OverlayRenderer.beginDraw(AllDefaultOverlayComponents.mainHud, context);
    }

    @Inject(method = "renderMainHud", at = @At("RETURN"))
    private void drawEndMainHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        OverlayRenderer.endDraw(AllDefaultOverlayComponents.mainHud, context);
    }
}


