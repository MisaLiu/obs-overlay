package me.zziger.obsoverlay.mixin.hud;

import me.zziger.obsoverlay.registry.AllDefaultOverlayComponents;
import me.zziger.obsoverlay.OverlayRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// priority = 1500 (> ImmediatelyFast's default 1000) so our HEAD injection runs
// AFTER IF's MixinChatHud.beginBatching(), which calls drawContext.draw() on the
// main framebuffer before replacing vertexConsumers. If our overlayFB.beginWrite()
// ran first, that draw() would incorrectly flush main-framebuffer content into the
// overlay FBO. With priority 1500 our HEAD always follows IF's setup, and our RETURN
// always follows IF's endBatching() which flushes batched chat vertices into our
// overlay FBO (since it is still the current draw target at that point).
@Mixin(value = ChatHud.class, priority = 1500)
public class ChatHudMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void drawStart(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        OverlayRenderer.beginDraw(AllDefaultOverlayComponents.chat, context);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void drawEnd(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        OverlayRenderer.endDraw(AllDefaultOverlayComponents.chat, context);
    }
}
