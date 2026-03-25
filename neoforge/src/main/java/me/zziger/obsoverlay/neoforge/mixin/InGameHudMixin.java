package me.zziger.obsoverlay.neoforge.mixin;

import me.zziger.obsoverlay.OverlayRenderer;
import me.zziger.obsoverlay.registry.AllDefaultOverlayComponents;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    // NeoForge exposes individual render sub-methods that do not receive a DrawContext
    // parameter, so we pass null. ImmediatelyFastCompat.forceDraw(null) is a no-op,
    // which is acceptable because NeoForge ships its own ImmediatelyFast build that
    // may handle batching differently. ExordiumCompat.blitExordiumBufferIfCapturing()
    // handles the null overlayFramebuffer guard internally.

    @Inject(method = "renderHotbar", at = @At(value = "HEAD"))
    private void drawStartHotbar(CallbackInfo ci) {
        OverlayRenderer.beginDraw(AllDefaultOverlayComponents.mainHud, null);
    }

    @Inject(method = "renderHotbar", at = @At(value = "RETURN"))
    private void drawEndHotbar(CallbackInfo ci) {
        OverlayRenderer.endDraw(AllDefaultOverlayComponents.mainHud, null);
    }

    @Inject(method = "maybeRenderJumpMeter", at = @At(value = "HEAD"))
    private void drawStartJumpMeter(CallbackInfo ci) {
        OverlayRenderer.beginDraw(AllDefaultOverlayComponents.mainHud, null);
    }

    @Inject(method = "maybeRenderJumpMeter", at = @At(value = "RETURN"))
    private void drawEndJumpMeter(CallbackInfo ci) {
        OverlayRenderer.endDraw(AllDefaultOverlayComponents.mainHud, null);
    }

    @Inject(method = "maybeRenderExperienceBar", at = @At(value = "HEAD"))
    private void drawStartExperienceBar(CallbackInfo ci) {
        OverlayRenderer.beginDraw(AllDefaultOverlayComponents.mainHud, null);
    }

    @Inject(method = "maybeRenderExperienceBar", at = @At(value = "RETURN"))
    private void drawEndExperienceBar(CallbackInfo ci) {
        OverlayRenderer.endDraw(AllDefaultOverlayComponents.mainHud, null);
    }

    @Inject(method = "renderHealthLevel", at = @At(value = "HEAD"))
    private void drawStartPlayerHealth(CallbackInfo ci) {
        OverlayRenderer.beginDraw(AllDefaultOverlayComponents.mainHud, null);
    }

    @Inject(method = "renderHealthLevel", at = @At(value = "RETURN"))
    private void drawEndPlayerHealth(CallbackInfo ci) {
        OverlayRenderer.endDraw(AllDefaultOverlayComponents.mainHud, null);
    }

    @Inject(method = "renderArmorLevel", at = @At(value = "HEAD"))
    private void drawStartPlayerArmor(CallbackInfo ci) {
        OverlayRenderer.beginDraw(AllDefaultOverlayComponents.mainHud, null);
    }

    @Inject(method = "renderArmorLevel", at = @At(value = "RETURN"))
    private void drawEndPlayerArmor(CallbackInfo ci) {
        OverlayRenderer.endDraw(AllDefaultOverlayComponents.mainHud, null);
    }

    @Inject(method = "renderFoodLevel", at = @At(value = "HEAD"))
    private void drawStartPlayerFood(CallbackInfo ci) {
        OverlayRenderer.beginDraw(AllDefaultOverlayComponents.mainHud, null);
    }

    @Inject(method = "renderFoodLevel", at = @At(value = "RETURN"))
    private void drawEndPlayerFood(CallbackInfo ci) {
        OverlayRenderer.endDraw(AllDefaultOverlayComponents.mainHud, null);
    }

    @Inject(method = "maybeRenderVehicleHealth", at = @At(value = "HEAD"))
    private void drawStartVehicleHealth(CallbackInfo ci) {
        OverlayRenderer.beginDraw(AllDefaultOverlayComponents.mainHud, null);
    }

    @Inject(method = "maybeRenderVehicleHealth", at = @At(value = "RETURN"))
    private void drawEndVehicleHealth(CallbackInfo ci) {
        OverlayRenderer.endDraw(AllDefaultOverlayComponents.mainHud, null);
    }

    @Inject(method = "maybeRenderSelectedItemName", at = @At(value = "HEAD"))
    private void drawStartSelectedItemName(CallbackInfo ci) {
        OverlayRenderer.beginDraw(AllDefaultOverlayComponents.mainHud, null);
    }

    @Inject(method = "maybeRenderSelectedItemName", at = @At(value = "RETURN"))
    private void drawEndSelectedItemName(CallbackInfo ci) {
        OverlayRenderer.endDraw(AllDefaultOverlayComponents.mainHud, null);
    }

    @Inject(method = "maybeRenderSpectatorTooltip", at = @At(value = "HEAD"))
    private void drawStartSpectatorTooltip(CallbackInfo ci) {
        OverlayRenderer.beginDraw(AllDefaultOverlayComponents.mainHud, null);
    }

    @Inject(method = "maybeRenderSpectatorTooltip", at = @At(value = "RETURN"))
    private void drawEndSpectatorTooltip(CallbackInfo ci) {
        OverlayRenderer.endDraw(AllDefaultOverlayComponents.mainHud, null);
    }
}
