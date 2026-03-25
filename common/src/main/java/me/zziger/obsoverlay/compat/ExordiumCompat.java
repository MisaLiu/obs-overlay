package me.zziger.obsoverlay.compat;

import dev.architectury.platform.Platform;

/**
 * Compatibility notes for <a href="https://modrinth.com/mod/exordium">Exordium</a>.
 *
 * <h3>Architecture conflict</h3>
 * Exordium renders each HUD component into its own private {@code TextureTarget}
 * (FBO) by hijacking {@code MinecraftClient.getFramebuffer()} to return its own FBO
 * during the capture window. It then draws all cached FBOs back to the screen in one
 * batched pass at the end of {@code InGameHud.render()}.
 *
 * <p>This conflicts with our overlay mechanism in two ways:
 * <ol>
 *   <li>Our {@code beginDraw()} binds {@code overlayFramebuffer} directly, but
 *       Exordium's {@code captureComponent()} then overrides the bound FBO to its
 *       own {@code guiTarget}. The actual rendering ends up in {@code guiTarget},
 *       not in our {@code overlayFramebuffer}.</li>
 *   <li>Our {@code endDraw()} calls {@code MinecraftClient.getFramebuffer().beginWrite()},
 *       which Exordium intercepts and returns {@code guiTarget} instead of the real
 *       main framebuffer during the capture window.</li>
 *   <li>Exordium's {@code DelayedRenderCallManager} always draws the component FBOs
 *       back to the main framebuffer. There is no hook to suppress this for
 *       components we want to separate.</li>
 * </ol>
 *
 * <p>As a result, HUD separation for Exordium-managed components (hotbar, health,
 * etc.) is not achievable without a deeper integration that would require either
 * forking Exordium or adding an explicit API to it. This is tracked as a known
 * compatibility limitation.
 *
 * <h3>What we do instead</h3>
 * We leave Exordium's render flow completely untouched. The only thing we ensure is
 * that our overlay mechanism does not interfere with Exordium's rendering (e.g.
 * by not corrupting GL shader state or framebuffer bindings during Exordium's
 * capture window).
 */
public class ExordiumCompat {

    private static Boolean present = null;

    /** Returns {@code true} if Exordium is loaded in the current game instance. */
    public static boolean isPresent() {
        if (present == null) {
            present = Platform.isModLoaded("exordium");
        }
        return present;
    }
}
