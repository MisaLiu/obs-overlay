package me.zziger.obsoverlay.platform;

import com.sun.jna.Native;
import dev.architectury.platform.Platform;
import me.zziger.obsoverlay.OBSOverlay;
import me.zziger.obsoverlay.modules.GLXHook;
import net.minecraft.client.MinecraftClient;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class LinuxHook implements PlatformHook {
    private Runnable renderCallback;
    private boolean initialized = false;

    @Override
    public boolean initialize(MinecraftClient client) {
        if (!loadLibrary()) {
            return false;
        }

        try {
            GLXHook hook = Native.load("glx_hook", GLXHook.class);
            hook.InitHook();
            hook.SetSwapCallback(() -> {
                if (renderCallback != null) {
                    renderCallback.run();
                }
            });
            hook.EnableHook();

            initialized = true;
            return true;
        } catch (Exception e) {
            OBSOverlay.LOGGER.error("Failed to initialize Linux hook", e);
            return false;
        }
    }

    @Override
    public void setRenderCallback(Runnable callback) {
        this.renderCallback = callback;
    }

    @Override
    public boolean isSupported() {
        String arch = System.getProperty("os.arch").toLowerCase();
        return arch.equals("x86_64") || arch.equals("amd64");
    }

    @Override
    public String getPlatformName() {
        return "Linux";
    }

    private boolean loadLibrary() {
        InputStream libFile = LinuxHook.class.getResourceAsStream("/lib/glx_hook.x64.so");
        if (libFile == null) {
            OBSOverlay.LOGGER.error("Failed to get glx_hook.so");
            return false;
        }

        File nativeDir = new File(Platform.getGameFolder().toAbsolutePath().toString().concat("/native"));
        File copyLibFile = new File(Platform.getGameFolder().toAbsolutePath().toString().concat("/native/libglx_hook.so"));
        nativeDir.mkdir();

        try {
            FileOutputStream fos = new FileOutputStream(copyLibFile);
            copyLibFile.createNewFile();
            IOUtils.copy(libFile, fos);
            fos.close();
        } catch (Exception e) {
            OBSOverlay.LOGGER.error("Failed to copy glx_hook.so", e);
            return false;
        }

        System.setProperty("jna.library.path", nativeDir.getAbsolutePath());
        return true;
    }
}
