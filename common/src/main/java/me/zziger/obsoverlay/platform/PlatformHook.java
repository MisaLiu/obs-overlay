package me.zziger.obsoverlay.platform;

import net.minecraft.client.MinecraftClient;

public interface PlatformHook {
    boolean initialize(MinecraftClient client);
    void setRenderCallback(Runnable callback);

    /**
     * Called at the end of each rendered frame (via Mixin on MinecraftClient.render RETURN).
     * Platform implementations that manage their own presentation window (e.g. Linux GLFW)
     * should blit the overlay framebuffer to their secondary window here.
     * The default no-op is suitable for hook-based implementations (e.g. Windows wglSwapBuffers).
     *
     * @param dirty {@code true} if the overlay framebuffer was written to during this frame
     *              and the secondary window must be updated; {@code false} if the framebuffer
     *              is unchanged and the platform hook may skip the blit and swap entirely.
     */
    default void presentFrame(boolean dirty) {}

    boolean isSupported();
    String getPlatformName();
}
