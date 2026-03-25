package me.zziger.obsoverlay.platform;

import me.zziger.obsoverlay.OBSOverlay;
import me.zziger.obsoverlay.OverlayRenderer;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWWindowPosCallback;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Linux platform hook: creates a secondary GLFW window that presents the overlay
 * framebuffer each frame, keeping itself aligned with Minecraft's main window.
 *
 * <h3>Window property design decisions</h3>
 * <ul>
 *   <li><b>Mouse passthrough</b> – {@code GLFW_MOUSE_PASSTHROUGH} lets all pointer
 *       events fall through to whatever is underneath, so the overlay never blocks
 *       the user from interacting with the game.</li>
 *   <li><b>Position / size tracking</b> – GLFW position and size callbacks are
 *       registered on the <em>main</em> window. Whenever Minecraft moves or is
 *       resized, the callbacks fire and we immediately sync the secondary window.
 *       This is event-driven and has zero per-frame overhead (no polling).</li>
 *   <li><b>Viewport vs. window size</b> – GLFW distinguishes between the logical
 *       window size (screen coordinates, used by {@code glfwSetWindowSize}) and the
 *       framebuffer size (pixels, used by {@code glViewport}). On HiDPI displays
 *       these differ by the content-scale factor. The secondary window's
 *       {@code fbWidth}/{@code fbHeight} are tracked via its own
 *       {@code glfwSetFramebufferSizeCallback} so that the GL viewport always uses
 *       the correct pixel dimensions regardless of display scaling. Using the main
 *       window's framebuffer size for the secondary window's viewport would produce
 *       incorrect mouse coordinate mapping whenever the windows have different
 *       effective scales.</li>
 *   <li><b>Secondary-context shader + VAO</b> – VAOs are context-local and are not
 *       shared across contexts. Using Minecraft's {@code BufferRenderer} /
 *       {@code ShaderProgram} objects after switching context causes a SIGSEGV in
 *       the GPU driver. The fix is to compile a minimal passthrough shader and
 *       create a dedicated VAO while the secondary context is current.</li>
 * </ul>
 */
public class LinuxGLFWHook implements PlatformHook {

    // -------------------------------------------------------------------------
    // GLSL sources for the secondary-context blit shader
    // -------------------------------------------------------------------------

    private static final String VERT_SRC =
            "#version 150 core\n" +
            "out vec2 v_uv;\n" +
            "void main() {\n" +
            // Full-screen triangle: covers the viewport with 3 vertices, no VBO.
            "    vec2 pos = vec2((gl_VertexID & 1) * 2, (gl_VertexID >> 1) * 2);\n" +
            "    v_uv        = pos;\n" +
            "    gl_Position = vec4(pos * 2.0 - 1.0, 0.0, 1.0);\n" +
            "}\n";

    private static final String FRAG_SRC =
            "#version 150 core\n" +
            "in  vec2      v_uv;\n" +
            "uniform sampler2D u_texture;\n" +
            "out vec4      fragColor;\n" +
            "void main() {\n" +
            "    fragColor = texture(u_texture, v_uv);\n" +
            "}\n";

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** GLFW handle of the secondary (overlay) window. 0 = not yet created. */
    private long secondWindowHandle = 0L;

    /** GLFW handle of Minecraft's main window, cached in {@link #initialize}. */
    private long mainWindowHandle = 0L;

    /**
     * Logical size of the secondary window in screen coordinates.
     * Updated by the main-window <em>size</em> callback.
     * Used exclusively for {@code glfwSetWindowSize} – the window manager works in
     * logical units. Do NOT use these for {@code glViewport}; see {@link #secondFbWidth}.
     */
    private int windowWidth  = 0;
    private int windowHeight = 0;

    /**
     * Pixel (framebuffer) size of the <em>secondary</em> window.
     * Updated by the secondary window's own {@code glfwSetFramebufferSizeCallback}.
     * On non-HiDPI displays this equals {@code windowWidth/Height}; on HiDPI displays
     * it is {@code windowWidth/Height * contentScale}.
     * This is the only value that should be passed to {@code glViewport}.
     */
    private volatile int secondFbWidth  = 0;
    private volatile int secondFbHeight = 0;

    /**
     * Shader program compiled inside the secondary context.
     * Completely independent of Minecraft's shader objects.
     */
    private int blitProgram = 0;

    /** Uniform location of {@code u_texture} inside {@link #blitProgram}. */
    private int texUniformLocation = -1;

    /**
     * VAO created inside the secondary context.
     * Required by OpenGL core profile even though no vertex attributes are used
     * (positions are generated procedurally in the vertex shader).
     */
    private int blitVao = 0;

    /**
     * Cached {@link GLCapabilities} for each context.
     *
     * <p>{@link GL#createCapabilities()} queries every GL extension string and
     * resolves thousands of function pointers – it is far too expensive to call
     * every frame. LWJGL stores capabilities in a {@link ThreadLocal}; after the
     * first call per context we capture the result with {@link GL#getCapabilities()}
     * and restore it with {@link GL#setCapabilities(GLCapabilities)} on subsequent
     * context switches, reducing the per-switch cost to a single {@code ThreadLocal}
     * write.
     */
    private GLCapabilities mainContextCaps   = null;
    private GLCapabilities secondContextCaps = null;

    /**
     * Retained references to the GLFW callbacks registered on the main window
     * and on the secondary window.
     * LWJGL callbacks are native objects; they must be {@link AutoCloseable#close()}d
     * to release the underlying native memory when they are no longer needed.
     *
     * <p><b>IMPORTANT – callback chaining:</b> Minecraft's {@code Window} class
     * ({@code class_1041}) registers its own {@code glfwSetWindowSizeCallback} and
     * {@code glfwSetWindowPosCallback} on the main window to keep its internal
     * width/height/x/y fields up to date. {@code glfwSetWindowXxxCallback} replaces
     * the existing callback and returns the previous one. If we simply replace those
     * callbacks with our own lambdas and discard the originals, Minecraft's
     * {@code Window} object stops receiving resize/move events, its cached dimensions
     * become stale, and its mouse-coordinate mapping breaks after any resize.
     *
     * <p>The fix is to capture the previous callback returned by each
     * {@code glfwSetWindowXxxCallback} call and invoke it from inside our wrapper,
     * preserving the full callback chain.
     */
    private GLFWWindowPosCallback      mainWindowPosCallback;
    private GLFWWindowSizeCallback     mainWindowSizeCallback;
    private GLFWFramebufferSizeCallback secondFbSizeCallback;

    // renderCallback is unused on Linux – rendering is driven by presentFrame()
    // called from the Mixin, not via a C-level swap hook.
    private Runnable renderCallback;

    // -------------------------------------------------------------------------
    // PlatformHook implementation
    // -------------------------------------------------------------------------

    @Override
    public boolean initialize(MinecraftClient client) {
        mainWindowHandle = client.getWindow().getHandle();

        // Read the initial logical window size (screen coordinates, not pixels).
        int[] wx = {0}, wy = {0};
        GLFW.glfwGetWindowSize(mainWindowHandle, wx, wy);
        windowWidth  = wx[0];
        windowHeight = wy[0];

        // ── GLFW window hints ──────────────────────────────────────────────────
        GLFW.glfwDefaultWindowHints();

        // Transparent framebuffer: fully-transparent pixels let the desktop show
        // through, so the overlay "floats" above the game without a background box.
        GLFW.glfwWindowHint(GLFW.GLFW_TRANSPARENT_FRAMEBUFFER, GLFW.GLFW_TRUE);

        // Always-on-top: the overlay stays above the game window.
        GLFW.glfwWindowHint(GLFW.GLFW_FLOATING, GLFW.GLFW_TRUE);

        // No title bar or borders – HUD content fills the entire window.
        GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);

        // Do not steal keyboard/mouse focus when shown or when the window first appears.
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED,       GLFW.GLFW_FALSE);

        // Share the GL context with Minecraft so that shared objects (textures,
        // VBOs) – including overlayFramebuffer's colour attachment – are directly
        // accessible without any cross-context copies.
        secondWindowHandle = GLFW.glfwCreateWindow(
                windowWidth, windowHeight,
                "OBS Overlay",
                0L,              // windowed (no monitor)
                mainWindowHandle // context-sharing partner
        );

        if (secondWindowHandle == 0L) {
            OBSOverlay.LOGGER.error("[LinuxGLFWHook] Failed to create secondary GLFW window");
            return false;
        }

        // ── Post-creation window attributes ───────────────────────────────────

        // Mouse passthrough: all pointer events fall through to whatever is below,
        // so the overlay never blocks the user from clicking inside the game.
        GLFW.glfwSetWindowAttrib(secondWindowHandle,
                GLFW.GLFW_MOUSE_PASSTHROUGH, GLFW.GLFW_TRUE);

        // ── Align with the main window's current position ──────────────────────
        int[] mx = {0}, my = {0};
        GLFW.glfwGetWindowPos(mainWindowHandle, mx, my);
        GLFW.glfwSetWindowPos(secondWindowHandle, mx[0], my[0]);

        // ── Register callbacks on the MAIN window ─────────────────────────────
        // We listen to the main window's move/resize events and mirror them to the
        // secondary window. This is event-driven; there is no per-frame polling.
        //
        // CRITICAL – callback chaining:
        // glfwSetWindowXxxCallback *replaces* the existing callback and returns the
        // previous one. Minecraft's Window class (class_1041) registers its own
        // glfwSetWindowSizeCallback and glfwSetWindowPosCallback on the main window
        // to keep its internal width/height/x/y fields current; these fields drive
        // mouse-coordinate mapping. If we simply overwrite those callbacks and discard
        // the originals, Minecraft's Window stops receiving events, its cached
        // dimensions become stale, and mouse coordinates break after any resize.
        //
        // Fix: install a temporary no-op to *capture* the current (Minecraft's)
        // callback, then install our real wrapper that chains to it.

        // Position callback
        final GLFWWindowPosCallback mcPosCallback =
                GLFW.glfwSetWindowPosCallback(mainWindowHandle, (w, x, y) -> {});
        mainWindowPosCallback = GLFW.glfwSetWindowPosCallback(mainWindowHandle,
                (window, xPos, yPos) -> {
                    // Forward to Minecraft's original callback FIRST so its internal
                    // state is updated before anything else reads it.
                    if (mcPosCallback != null) mcPosCallback.invoke(window, xPos, yPos);
                    GLFW.glfwSetWindowPos(secondWindowHandle, xPos, yPos);
                });

        // Size callback
        final GLFWWindowSizeCallback mcSizeCallback =
                GLFW.glfwSetWindowSizeCallback(mainWindowHandle, (w, ww, hh) -> {});
        mainWindowSizeCallback = GLFW.glfwSetWindowSizeCallback(mainWindowHandle,
                (window, width, height) -> {
                    // Forward to Minecraft's original callback FIRST.
                    if (mcSizeCallback != null) mcSizeCallback.invoke(window, width, height);
                    windowWidth  = width;
                    windowHeight = height;
                    GLFW.glfwSetWindowSize(secondWindowHandle, width, height);
                });

        // Framebuffer-size callback on the SECONDARY window.
        // The framebuffer size (pixels) can differ from the logical window size
        // (screen coordinates) on HiDPI displays. We must use the framebuffer size
        // for glViewport so that the rendered content exactly fills the window and
        // mouse coordinate mapping stays correct after any resize or scale change.
        secondFbSizeCallback = GLFW.glfwSetFramebufferSizeCallback(secondWindowHandle,
                (window, fbW, fbH) -> {
                    secondFbWidth  = fbW;
                    secondFbHeight = fbH;
                });

        // Seed the secondary framebuffer size from its current value so that the
        // very first frame has a valid viewport even before any resize event fires.
        int[] fbW = {0}, fbH = {0};
        GLFW.glfwGetFramebufferSize(secondWindowHandle, fbW, fbH);
        secondFbWidth  = fbW[0];
        secondFbHeight = fbH[0];

        // ── Build secondary-context GL resources ──────────────────────────────
        // VAOs and shader programs are context-local objects. They must be created
        // while the secondary context is current.
        GLFW.glfwMakeContextCurrent(secondWindowHandle);
        // createCapabilities() is expensive (queries all GL extensions); call it
        // once here and cache the result for use in presentFrame().
        secondContextCaps = GL.createCapabilities();

        // Disable VSync on the secondary context.
        // glfwSwapInterval acts on the *current* context. A freshly created context
        // has an undefined (often driver-default = 1) swap interval. If we leave it
        // at 1, every glfwSwapBuffers(secondWindow) in presentFrame() blocks for a
        // full VBlank, effectively halving the game's maximum framerate because the
        // main window's own VSync wait then stacks on top. Setting it to 0 lets the
        // secondary swap return immediately; VSync for the main window is controlled
        // exclusively by Minecraft's own glfwSwapInterval call.
        GLFW.glfwSwapInterval(0);

        if (!buildSecondaryContextResources()) {
            GLFW.glfwMakeContextCurrent(mainWindowHandle);
            mainContextCaps = GL.createCapabilities();
            return false;
        }

        // Restore Minecraft's main context before returning to the caller.
        // Cache its capabilities for cheap restoration in presentFrame().
        GLFW.glfwMakeContextCurrent(mainWindowHandle);
        mainContextCaps = GL.createCapabilities();

        OBSOverlay.LOGGER.info("[LinuxGLFWHook] Secondary GLFW window created (handle={})",
                secondWindowHandle);
        return true;
    }

    /**
     * Compiles {@link #blitProgram} and allocates {@link #blitVao}.
     * <p><b>Must be called while the secondary context is current.</b>
     *
     * @return {@code true} on success; {@code false} if compilation or linking failed.
     */
    private boolean buildSecondaryContextResources() {
        // Vertex shader
        int vert = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vert, VERT_SRC);
        glCompileShader(vert);
        if (glGetShaderi(vert, GL_COMPILE_STATUS) == GL_FALSE) {
            OBSOverlay.LOGGER.error("[LinuxGLFWHook] Vertex shader compilation failed:\n{}",
                    glGetShaderInfoLog(vert));
            glDeleteShader(vert);
            return false;
        }

        // Fragment shader
        int frag = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(frag, FRAG_SRC);
        glCompileShader(frag);
        if (glGetShaderi(frag, GL_COMPILE_STATUS) == GL_FALSE) {
            OBSOverlay.LOGGER.error("[LinuxGLFWHook] Fragment shader compilation failed:\n{}",
                    glGetShaderInfoLog(frag));
            glDeleteShader(vert);
            glDeleteShader(frag);
            return false;
        }

        // Link program
        blitProgram = glCreateProgram();
        glAttachShader(blitProgram, vert);
        glAttachShader(blitProgram, frag);
        glLinkProgram(blitProgram);
        // Shaders are no longer needed once linked.
        glDeleteShader(vert);
        glDeleteShader(frag);

        if (glGetProgrami(blitProgram, GL_LINK_STATUS) == GL_FALSE) {
            OBSOverlay.LOGGER.error("[LinuxGLFWHook] Shader program linking failed:\n{}",
                    glGetProgramInfoLog(blitProgram));
            glDeleteProgram(blitProgram);
            blitProgram = 0;
            return false;
        }

        texUniformLocation = glGetUniformLocation(blitProgram, "u_texture");

        // VAO (required by OpenGL core profile even with no vertex attributes)
        blitVao = glGenVertexArrays();

        return true;
    }

    @Override
    public void setRenderCallback(Runnable callback) {
        this.renderCallback = callback;
    }

    /**
     * Called at the end of every rendered frame by the Mixin injection on
     * {@code MinecraftClient.render(Z)V RETURN}.
     *
     * <p>Performance notes:
     * <ul>
     *   <li>{@code glfwMakeContextCurrent} performs an implicit GL flush on the
     *       outgoing context per the OpenGL spec, so no explicit {@code glFlush()}
     *       is needed before the switch.</li>
     *   <li>Capabilities are restored with {@link GL#setCapabilities} (a single
     *       {@code ThreadLocal} write) instead of {@link GL#createCapabilities}
     *       (thousands of extension queries), using the instances cached during
     *       {@link #initialize}.</li>
     *   <li>{@code glfwPollEvents()} is omitted here because
     *       {@code RenderSystem.flipFrame()} already calls it twice per frame
     *       (before and after the main window swap). Our position/size callbacks
     *       are dispatched during those calls.</li>
     *   <li>The secondary window's swap interval is 0 (set in {@link #initialize}),
     *       so {@code glfwSwapBuffers(secondWindow)} returns immediately without
     *       blocking on a VBlank, which would otherwise cap the game's framerate
     *       at the monitor refresh rate.</li>
     * </ul>
     */
    @Override
    public void presentFrame(boolean dirty) {
        if (!dirty) return;  // overlay unchanged this frame – skip context switch entirely

        if (secondWindowHandle == 0L) return;
        if (blitProgram == 0 || blitVao == 0) return;
        if (OverlayRenderer.getOverlayFramebuffer() == null) return;

        int overlayTexture = OverlayRenderer.getOverlayFramebuffer().getColorAttachment();
        if (overlayTexture == 0) return;

        int vpWidth  = secondFbWidth;
        int vpHeight = secondFbHeight;
        if (vpWidth == 0 || vpHeight == 0) return;

        // Switch to the secondary window's context.
        // glfwMakeContextCurrent implicitly flushes the outgoing context's command
        // queue per the OpenGL spec – no explicit glFlush() is needed beforehand.
        GLFW.glfwMakeContextCurrent(secondWindowHandle);
        // Restore cached capabilities – O(1) ThreadLocal write, not a full query.
        GL.setCapabilities(secondContextCaps);

        // Draw the overlay texture to the secondary window's back buffer.
        glViewport(0, 0, vpWidth, vpHeight);
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glClear(GL_COLOR_BUFFER_BIT);

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glUseProgram(blitProgram);
        // Texture is shared via context sharing – the ID is valid in this context.
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, overlayTexture);
        glUniform1i(texUniformLocation, 0);

        // Full-screen triangle: positions generated in the vertex shader via gl_VertexID.
        glBindVertexArray(blitVao);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);

        glUseProgram(0);
        glBindTexture(GL_TEXTURE_2D, 0);

        // Present – swap interval is 0 so this returns immediately (no VBlank wait).
        GLFW.glfwSwapBuffers(secondWindowHandle);

        // Restore Minecraft's main context.
        GLFW.glfwMakeContextCurrent(mainWindowHandle);
        GL.setCapabilities(mainContextCaps);

        // glfwPollEvents() is intentionally omitted: RenderSystem.flipFrame() already
        // calls it twice per frame (before and after glfwSwapBuffers on the main window).
        // Calling it again here would redundantly re-process the event queue.
    }

    @Override
    public boolean isSupported() {
        // Pure Java + LWJGL; no native library compiled for a specific ABI is needed.
        return true;
    }

    @Override
    public String getPlatformName() {
        return "Linux";
    }
}
