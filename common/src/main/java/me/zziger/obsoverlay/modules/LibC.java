package me.zziger.obsoverlay.modules;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface LibC extends Library {
    LibC INSTANCE = Native.load("c", LibC.class);

    Pointer dlopen(String filename, int flag);
    Pointer dlsym(Pointer handle, String symbol);
    String dlerror();
    int dlclose(Pointer handle);

    int RTLD_LAZY = 0x00001;
    int RTLD_NOW = 0x00002;
    int RTLD_GLOBAL = 0x00100;
}
