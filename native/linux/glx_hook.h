#ifndef GLX_HOOK_H
#define GLX_HOOK_H

typedef void (*SwapCallback)(void);

int InitHook(void);
int SetSwapCallback(SwapCallback callback);
int EnableHook(void);

#endif
