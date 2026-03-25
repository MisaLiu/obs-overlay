package me.zziger.obsoverlay.modules;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Pointer;

public interface GLXHook extends Library {
    interface SwapCallback extends Callback {
        void callback();
    }

    int InitHook();
    int SetSwapCallback(SwapCallback callback);
    int EnableHook();
}
