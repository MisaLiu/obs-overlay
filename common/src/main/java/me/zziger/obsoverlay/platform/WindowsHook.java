package me.zziger.obsoverlay.platform;

import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import dev.architectury.platform.Platform;
import me.zziger.obsoverlay.OBSOverlay;
import me.zziger.obsoverlay.modules.Kernel32;
import me.zziger.obsoverlay.modules.MinHook;
import net.minecraft.client.MinecraftClient;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class WindowsHook implements PlatformHook {
    private PointerByReference reference;
    private Runnable renderCallback;
    private boolean initialized = false;

    @Override
    public boolean initialize(MinecraftClient client) {
        if (!loadLibrary()) {
            return false;
        }

        try {
            Pointer module = Kernel32.INSTANCE.GetModuleHandleA("opengl32.dll");
            Pointer proc = Kernel32.INSTANCE.GetProcAddress(module, "wglSwapBuffers");

            MinHook minhook = Native.load("MinHook", MinHook.class);
            minhook.MH_Initialize();
            reference = new PointerByReference();

            minhook.MH_CreateHook(proc, hDc -> {
                if (renderCallback != null) {
                    renderCallback.run();
                }
                Function origFunction = Function.getFunction(reference.getValue(), Function.ALT_CONVENTION);
                return (boolean) origFunction.invoke(Boolean.class, new Object[]{hDc});
            }, reference);
            minhook.MH_EnableHook(proc);

            initialized = true;
            return true;
        } catch (Exception e) {
            OBSOverlay.LOGGER.error("Failed to initialize Windows hook", e);
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
        return !arch.contains("aarch");
    }

    @Override
    public String getPlatformName() {
        return "Windows";
    }

    private boolean loadLibrary() {
        String arch = System.getProperty("os.arch").toLowerCase();
        boolean is64 = arch.equals("x86_64") || arch.equals("amd64") || arch.equals("x64") || arch.equals("ia64");
        
        InputStream libFile = WindowsHook.class.getResourceAsStream(is64 ? "/lib/MinHook.x64.dll" : "/lib/MinHook.x86.dll");
        if (libFile == null) {
            OBSOverlay.LOGGER.error("Failed to get MinHook DLL");
            return false;
        }

        File nativeDir = new File(Platform.getGameFolder().toAbsolutePath().toString().concat("/native"));
        File copyLibFile = new File(Platform.getGameFolder().toAbsolutePath().toString().concat("/native/MinHook.dll"));
        nativeDir.mkdir();

        try {
            FileOutputStream fos = new FileOutputStream(copyLibFile);
            copyLibFile.createNewFile();
            IOUtils.copy(libFile, fos);
            fos.close();
        } catch (Exception e) {
            OBSOverlay.LOGGER.error("Failed to copy MinHook DLL", e);
            return false;
        }

        System.setProperty("jna.library.path", nativeDir.getAbsolutePath());
        return true;
    }
}
