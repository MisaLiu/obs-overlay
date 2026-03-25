package me.zziger.obsoverlay.platform;

import me.zziger.obsoverlay.OBSOverlay;

public class PlatformManager {
    private static PlatformHook instance;

    public static PlatformHook getInstance() {
        if (instance == null) {
            String os = System.getProperty("os.name").toLowerCase();
            
            if (os.contains("win")) {
                instance = new WindowsHook();
            } else if (os.contains("nux") || os.contains("nix")) {
                instance = new LinuxGLFWHook();
            } else {
                OBSOverlay.LOGGER.error("Unsupported operating system: " + os);
                instance = new UnsupportedHook();
            }
        }
        return instance;
    }

    private static class UnsupportedHook implements PlatformHook {
        @Override
        public boolean initialize(net.minecraft.client.MinecraftClient client) {
            return false;
        }

        @Override
        public void setRenderCallback(Runnable callback) {}

        @Override
        public boolean isSupported() {
            return false;
        }

        @Override
        public String getPlatformName() {
            return "Unsupported";
        }
    }
}
