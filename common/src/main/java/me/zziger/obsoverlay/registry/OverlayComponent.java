package me.zziger.obsoverlay.registry;

public interface OverlayComponent {

    /**
     * Can this component auto-hide when any screen is open?
     * This is useful for stuff like F3 menu, which would be annoying over screens.
     *
     * @return If the component can auto-hide
     */
    boolean canAutoHide();

    /**
     * @return The unique ID of this component. Used for localization and config
     */
    String getId();

    /**
     * @return Is overlaying enabled for this component by default
     */
    boolean isOverlayEnabledDefault();

    /**
     * @return Is overlaying enabled for this component (in config)
     */
    boolean isOverlayEnabled();

    /**
     * Set overlaying state for this component (in config)
     */
    void setOverlayEnabled(boolean value);

    /**
     * @return Is auto-hide enabled for this component (in config)
     */
    boolean isAutoHideEnabled();

    /**
     * Set auto-hide state for this component (in config)
     */
    void setAutoHideEnabled(boolean value);

    /**
     * @return Is the component hidden (e.g. by auto-hide)
     */
    boolean isHidden();

    /**
     * Invoked before beginDraw.
     *
     * @param context the current {@link net.minecraft.client.gui.DrawContext}; used to
     *                flush any pending ImmediatelyFast batched draws before the render
     *                target is switched to the overlay framebuffer
     */
    default void beforeBeginDraw(net.minecraft.client.gui.DrawContext context) {};

    /**
     * Invoked before endDraw.
     *
     * @param context the current {@link net.minecraft.client.gui.DrawContext}; used to
     *                flush any pending ImmediatelyFast batched draws before the render
     *                target is switched back to the main framebuffer
     */
    default void beforeEndDraw(net.minecraft.client.gui.DrawContext context) {};
}
