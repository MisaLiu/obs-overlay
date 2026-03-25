package me.zziger.obsoverlay;

import com.mojang.blaze3d.systems.RenderSystem;
import me.zziger.obsoverlay.registry.AllDefaultOverlayComponents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OBSOverlay {
    public static final String MOD_ID = "obs_overlay";
    public static final Logger LOGGER = LoggerFactory.getLogger("obs_overlay");

    public static boolean libraryInitialized = false;

    public static void beforeScreenRender(Screen instance) {
        boolean overlay = OverlayUtils.isScreenOverlayed(instance);
        if (overlay) {
            OverlayRenderer.beginDraw();
            RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
        }
    }

    public static void afterScreenRender(Screen instance, DrawContext context) {
        context.draw();
        boolean overlay = OverlayUtils.isScreenOverlayed(instance);
        if (overlay) {
            OverlayRenderer.endDraw();
        }
    }


    public static void init() {
        OBSOverlayConfig.init();
        AllDefaultOverlayComponents.init();
    }
}
