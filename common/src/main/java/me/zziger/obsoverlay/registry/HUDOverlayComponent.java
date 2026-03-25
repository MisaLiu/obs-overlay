package me.zziger.obsoverlay.registry;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import me.zziger.obsoverlay.compat.ImmediatelyFastCompat;
import net.minecraft.client.gui.DrawContext;

public class HUDOverlayComponent extends DefaultOverlayComponent {
    public HUDOverlayComponent(String id, boolean defaultOverlay, boolean canAutoHide) {
        super(id, defaultOverlay, canAutoHide);
    }

    @Override
    public void beforeBeginDraw(DrawContext context) {
        // Flush any pending ImmediatelyFast batched vertices to the CURRENT
        // (main) framebuffer BEFORE we switch the draw target to overlayFramebuffer.
        // Note: DrawContext.draw() / tryDraw() are suppressed by IF's own Mixin
        // during the batch window, so we must use the direct BatchingBuffers API.
        ImmediatelyFastCompat.tryForceDrawBatches();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE);
    }

    @Override
    public void beforeEndDraw(DrawContext context) {
        // Flush again BEFORE restoring the main framebuffer, so that vertices
        // accumulated while overlayFramebuffer was bound are submitted to it,
        // not to the main framebuffer after the switch.
        ImmediatelyFastCompat.tryForceDrawBatches();
        RenderSystem.defaultBlendFunc();
    }
}

