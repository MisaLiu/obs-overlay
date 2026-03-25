package me.zziger.obsoverlay.mixin.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import me.zziger.obsoverlay.registry.AllDefaultOverlayComponents;
import me.zziger.obsoverlay.OverlayRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    // ChatScreen.render(DrawContext, int, int, float) – DrawContext is the first
    // explicit method parameter and can be declared directly in the handler
    // without @Local. MixinExtras requires any @Local ("sugared") parameters to
    // be trailing (after CallbackInfo), so we must NOT put DrawContext before ci
    // when using @Local. Since DrawContext is already a plain method arg here, we
    // simply include it in the handler signature before CallbackInfo as Mixin expects.
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;render(Lnet/minecraft/client/gui/DrawContext;IIIZ)V", shift = At.Shift.AFTER))
    private void drawStart(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        OverlayRenderer.beginDraw(AllDefaultOverlayComponents.chatBar, context);
        RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void drawEnd(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        OverlayRenderer.endDraw(AllDefaultOverlayComponents.chatBar, context);
    }
}
