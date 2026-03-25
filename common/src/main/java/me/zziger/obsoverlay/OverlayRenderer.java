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
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;

import java.util.Objects;

import static net.minecraft.client.MinecraftClient.IS_SYSTEM_MAC;
import static org.lwjgl.opengl.GL11.*;

public class OverlayRenderer {
    private static PlatformHook platformHook;
    private static boolean framebufferDirty = false;
    private static Framebuffer overlayFramebuffer = null;

    /**
     * The real main framebuffer, cached at init time.
     *
     * <p>We cache this reference because {@code MinecraftClient.getFramebuffer()} can
     * be intercepted by mods such as Exordium, which temporarily redirect it to their
     * own per-component FBO during a capture window. If we called
     * {@code getFramebuffer()} inside {@code endDraw()} we might restore the wrong
     * FBO as the draw target, leaving subsequent rendering in an incorrect state and
     * causing flicker or missing content in the overlay framebuffer.
     */
    private static Framebuffer mainFramebuffer = null;

    /**
     * Whether the overlay framebuffer has already been cleared at least once in the
     * current frame. The clear is deferred to the first actual {@link #beginDraw()}
     * call so that frames where no HUD component writes anything (e.g. Exordium or
     * Dynamic FPS skip frames) do not erase the content from the previous rendered
     * frame, which would cause the overlay window to flicker.
     */
    private static boolean frameCleared = false;

    public static void markFramebufferDirty() {
        framebufferDirty = true;
    }

    public static void beginDraw() {
        if (overlayFramebuffer == null) return;
        // Lazy-clear: only clear the framebuffer on the very first beginDraw() of
        // each frame. Subsequent calls within the same frame skip the clear so that
        // earlier components already written are preserved. Frames in which no
        // component calls beginDraw() keep the content from the last rendered frame.
        if (!frameCleared) {
            overlayFramebuffer.setClearColor(0, 0, 0, 0);
            overlayFramebuffer.clear(IS_SYSTEM_MAC);
            frameCleared = true;
        }
        overlayFramebuffer.beginWrite(false);
        framebufferDirty = true;
    }

    public static void beginEmptyDraw() {
        MinecraftClient.getInstance().getFramebuffer().endWrite();
    }

    public static void beginDraw(OverlayComponent component, DrawContext context) {
        if (!component.isOverlayEnabled()) return;
        component.beforeBeginDraw(context);
        if (component.isHidden()) beginEmptyDraw();
        else beginDraw();
    }

    public static void endDraw() {
        if (overlayFramebuffer == null) return;
        // Restore the real main framebuffer directly via the cached reference.
        // Do NOT use MinecraftClient.getFramebuffer() here: mods like Exordium
        // intercept that call and may return their own FBO during a capture window,
        // which would leave the draw target in the wrong state.
        if (mainFramebuffer != null) {
            mainFramebuffer.beginWrite(false);
        } else {
            MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
        }
        framebufferDirty = true;
    }

    public static void endDraw(OverlayComponent component, DrawContext context) {
        if (!component.isOverlayEnabled()) return;
        component.beforeEndDraw(context);
        endDraw();
    }

    public static void onResolutionChanged(MinecraftClient client) {
        if (overlayFramebuffer == null) return;
        // Refresh the main framebuffer reference: Minecraft recreates it on resize.
        mainFramebuffer = client.getFramebuffer();
        overlayFramebuffer.resize(client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight(), IS_SYSTEM_MAC);
    }

    public static void beginFrame() {
        // Reset the lazy-clear flag. The actual clear of overlayFramebuffer is
        // deferred to the first beginDraw() call of the frame so that frames in
        // which Exordium / Dynamic FPS / similar mods skip HUD rendering do not
        // wipe the framebuffer content, which would cause the overlay to flicker.
        frameCleared = false;
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

        // Cache the real main framebuffer before any mod can intercept getFramebuffer().
        mainFramebuffer = client.getFramebuffer();

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
