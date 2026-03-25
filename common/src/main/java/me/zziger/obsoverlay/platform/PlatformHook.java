package me.zziger.obsoverlay.platform;

import net.minecraft.client.MinecraftClient;

public interface PlatformHook {
    boolean initialize(MinecraftClient client);
    void setRenderCallback(Runnable callback);
    boolean isSupported();
    String getPlatformName();
}
