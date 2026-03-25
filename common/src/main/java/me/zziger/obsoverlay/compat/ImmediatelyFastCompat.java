package me.zziger.obsoverlay.compat;

import me.zziger.obsoverlay.OBSOverlay;
import net.minecraft.client.gui.DrawContext;

import java.lang.reflect.Method;

/**
 * Compatibility layer for ImmediatelyFast's HUD batching system.
 *
 * <h3>IF hud_batching architecture (1.21+)</h3>
 * ImmediatelyFast wraps the entire {@code InGameHud.render()} call inside
 * {@code BatchingBuffers.runBatched()}, which temporarily replaces
 * {@code DrawContext.vertexConsumers} with a {@code HudBatchingBufferSource}.
 * All vertex data is accumulated there and flushed in one shot at the end.
 *
 * <p>During this batch window, {@code DrawContext.draw()} / {@code tryDraw()} are
 * intercepted by IF's own Mixin ({@code MixinDrawContext.dontTryDrawIfBatching}) and
 * silently cancelled – calling them does nothing. This is why our previous approach
 * of calling {@code context.draw()} before switching framebuffers had no effect.
 *
 * <h3>The real problem</h3>
 * Our {@code beginDraw()} calls {@code overlayFramebuffer.beginWrite(false)}, which
 * binds a different GL draw framebuffer. IF does not know about this switch, so when
 * its batched vertices are eventually flushed (inside {@code runBatched()}'s own
 * {@code drawContext.draw()} call), the wrong framebuffer may be bound.
 *
 * IF does provide a hook: {@code BatchingBuffers.tryForceDrawHudBuffers()} will
 * immediately flush any pending batched vertices to the <em>currently bound</em>
 * framebuffer, provided the buffer source is not already drawing. Calling this:
 * <ul>
 *   <li><b>before</b> {@code overlayFramebuffer.beginWrite()} ensures the pending
 *       vertices land in the main framebuffer (where they belong).</li>
 *   <li><b>before</b> the return to the main framebuffer ensures vertices drawn while
 *       the overlay FBO was bound land in the overlay FBO (where they belong).</li>
 * </ul>
 *
 * <p>All IF access goes through reflection so the class compiles and loads correctly
 * when ImmediatelyFast is absent.
 */
public class ImmediatelyFastCompat {

    private static Boolean present      = null;
    private static boolean reflectReady = false;
    private static boolean reflectFailed = false;

    /** {@code BatchingBuffers.tryForceDrawHudBuffers()} reflective handle. */
    private static Method tryForceDrawHudBuffers = null;

    // -------------------------------------------------------------------------

    /** Returns {@code true} if ImmediatelyFast is loaded. */
    public static boolean isPresent() {
        if (present == null) {
            try {
                Class.forName("net.raphimc.immediatelyfast.ImmediatelyFast");
                present = true;
            } catch (ClassNotFoundException e) {
                present = false;
            }
        }
        return present;
    }

    /**
     * Resolves the reflective handle for
     * {@code BatchingBuffers.tryForceDrawHudBuffers()} on first use.
     */
    private static boolean ensureReflection() {
        if (reflectReady)  return true;
        if (reflectFailed) return false;

        try {
            Class<?> batchingBuffers = Class.forName(
                    "net.raphimc.immediatelyfast.feature.batching.BatchingBuffers");
            tryForceDrawHudBuffers = batchingBuffers.getMethod("tryForceDrawHudBuffers");
            reflectReady = true;
            return true;
        } catch (Exception e) {
            OBSOverlay.LOGGER.error(
                    "[ImmediatelyFastCompat] Failed to resolve BatchingBuffers – " +
                    "ImmediatelyFast compatibility will be disabled.", e);
            reflectFailed = true;
            return false;
        }
    }

    /**
     * Flushes any pending ImmediatelyFast batched vertices to the currently-bound
     * GL draw framebuffer.
     *
     * <p>This must be called:
     * <ol>
     *   <li><b>Before</b> binding {@code overlayFramebuffer} as the draw target, so
     *       that vertices accumulated up to this point land in the main framebuffer.</li>
     *   <li><b>Before</b> restoring the main framebuffer as the draw target, so that
     *       vertices accumulated while the overlay FBO was bound land in the overlay
     *       FBO.</li>
     * </ol>
     *
     * <p>This is a no-op when ImmediatelyFast is not installed, or when its internal
     * {@code HudBatchingBufferSource} is not currently active or is already drawing.
     */
    public static void tryForceDrawBatches() {
        if (!isPresent()) return;
        if (!ensureReflection()) return;

        try {
            tryForceDrawHudBuffers.invoke(null);
        } catch (Exception e) {
            OBSOverlay.LOGGER.error(
                    "[ImmediatelyFastCompat] tryForceDrawHudBuffers failed", e);
            reflectFailed = true;
        }
    }

    /**
     * @deprecated Use {@link #tryForceDrawBatches()} instead.
     *             {@code DrawContext.draw()} is intercepted and suppressed by
     *             ImmediatelyFast's own Mixin during the batch window and has no
     *             effect there.
     */
    @Deprecated
    public static void forceDraw(DrawContext context) {
        tryForceDrawBatches();
    }

    /** @deprecated Use {@link #tryForceDrawBatches()} instead. */
    @Deprecated
    public static void forceDraw() {
        tryForceDrawBatches();
    }
}
