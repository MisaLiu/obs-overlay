package me.zziger.obsoverlay;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import me.zziger.obsoverlay.platform.PlatformHook;
import me.zziger.obsoverlay.platform.PlatformManager;
import me.zziger.obsoverlay.registry.OverlayComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.*;

import java.util.Objects;

import static net.minecraft.client.MinecraftClient.IS_SYSTEM_MAC;
import static org.lwjgl.opengl.GL11.*;

public class OverlayRenderer {
    private static PlatformHook platformHook;
    private static boolean framebufferDirty = false;
    private static Framebuffer overlayFramebuffer = null;

    public static void markFramebufferDirty() {
        framebufferDirty = true;
    }

    public static void beginDraw() {
        if (overlayFramebuffer == null) return;
        overlayFramebuffer.beginWrite(false);
        framebufferDirty = true;
    }

    public static void beginEmptyDraw() {
        MinecraftClient.getInstance().getFramebuffer().endWrite();
    }

    public static void beginDraw(OverlayComponent component) {
        if (!component.isOverlayEnabled()) return;
        component.beforeBeginDraw();
        if (component.isHidden()) beginEmptyDraw();
        else beginDraw();
    }

    public static void endDraw() {
        if (overlayFramebuffer == null) return;
        MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
        framebufferDirty = true;
    }

    public static void endDraw(OverlayComponent component) {
        if (!component.isOverlayEnabled()) return;
        component.beforeEndDraw();
        endDraw();
    }

    public static void onResolutionChanged(MinecraftClient client) {
        if (overlayFramebuffer == null) return;
        overlayFramebuffer.resize(client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight(), IS_SYSTEM_MAC);
    }

    public static void beginFrame() {
        if (overlayFramebuffer == null) return;
        overlayFramebuffer.setClearColor(0, 0, 0, 0);
        overlayFramebuffer.clear(IS_SYSTEM_MAC);
    }

    /**
     * Returns the overlay framebuffer, or null if not yet initialized.
     * Intended for platform hooks that need direct access to the color attachment.
     */
    public static Framebuffer getOverlayFramebuffer() {
        return overlayFramebuffer;
    }

    /**
     * Blits the overlay framebuffer to the currently active GL context using the blit screen shader.
     * The caller is responsible for setting up an appropriate viewport beforehand.
     * This method is a no-op if the overlay framebuffer is null or has not been written to.
     */
    public static void blitOverlayToCurrentContext(int viewportWidth, int viewportHeight) {
        if (overlayFramebuffer == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        GlStateManager._disableDepthTest();
        GlStateManager._enableBlend();
        GlStateManager._blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager._viewport(0, 0, viewportWidth, viewportHeight);

        ShaderProgram shaderProgram = (ShaderProgram) Objects.requireNonNull(
                client.gameRenderer.blitScreenProgram, "Blit shader not loaded");
        shaderProgram.addSampler("DiffuseSampler", overlayFramebuffer.getColorAttachment());
        shaderProgram.bind();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.BLIT_SCREEN);
        bufferBuilder.vertex(0.0F, 0.0F, 0.0F);
        bufferBuilder.vertex(1.0F, 0.0F, 0.0F);
        bufferBuilder.vertex(1.0F, 1.0F, 0.0F);
        bufferBuilder.vertex(0.0F, 1.0F, 0.0F);
        BufferRenderer.draw(bufferBuilder.end());
        shaderProgram.unbind();
    }

    private static void renderFrame() {
        if (overlayFramebuffer != null && framebufferDirty) {
            framebufferDirty = false;
            MinecraftClient client = MinecraftClient.getInstance();
            blitOverlayToCurrentContext(
                    client.getWindow().getFramebufferWidth(),
                    client.getWindow().getFramebufferHeight());
        }
    }

    /**
     * Called at the end of each frame to give the active platform hook a chance
     * to present the overlay framebuffer to its secondary window (Linux GLFW path).
     * This is a no-op for hook-based platforms (Windows) where presentation is
     * driven by the intercepted swap call.
     *
     * <p>Passes the current {@code framebufferDirty} flag to the hook so that
     * implementations can skip an expensive context-switch + blit + swap cycle
     * when the overlay content has not changed since the last frame.
     */
    public static void presentFrame() {
        if (platformHook != null) {
            boolean dirty = framebufferDirty;
            framebufferDirty = false;
            platformHook.presentFrame(dirty);
        }
    }

    public static void init(MinecraftClient client) {
        platformHook = PlatformManager.getInstance();
        
        if (!platformHook.isSupported()) {
            OBSOverlay.LOGGER.error("OBS Overlay is not supported on " + platformHook.getPlatformName());
            return;
        }

        overlayFramebuffer = new SimpleFramebuffer(client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight(), true, IS_SYSTEM_MAC);
        RenderSystem.clearColor(0, 0, 0, 0);
        overlayFramebuffer.setClearColor(0, 0, 0, 0);
        overlayFramebuffer.clear(IS_SYSTEM_MAC);

        platformHook.setRenderCallback(OverlayRenderer::renderFrame);
        
        if (platformHook.initialize(client)) {
            OBSOverlay.libraryInitialized = true;
            OBSOverlay.LOGGER.info("OBS Overlay initialized successfully on " + platformHook.getPlatformName());
        } else {
            OBSOverlay.LOGGER.error("Failed to initialize OBS Overlay on " + platformHook.getPlatformName());
            overlayFramebuffer = null;
        }
    }
}
