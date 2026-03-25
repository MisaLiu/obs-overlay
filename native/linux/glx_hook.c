#define _GNU_SOURCE
#include <dlfcn.h>
#include <X11/Xlib.h>
#include <GL/glx.h>
#include "glx_hook.h"

static void (*original_glXSwapBuffers)(Display*, GLXDrawable) = NULL;
static SwapCallback java_callback = NULL;

int InitHook(void) {
    original_glXSwapBuffers = (void (*)(Display*, GLXDrawable))dlsym(RTLD_NEXT, "glXSwapBuffers");
    if (!original_glXSwapBuffers) {
        return -1;
    }
    return 0;
}

int SetSwapCallback(SwapCallback callback) {
    java_callback = callback;
    return 0;
}

int EnableHook(void) {
    return 0;
}

void glXSwapBuffers(Display* dpy, GLXDrawable drawable) {
    if (java_callback) {
        java_callback();
    }
    
    if (original_glXSwapBuffers) {
        original_glXSwapBuffers(dpy, drawable);
    }
}
