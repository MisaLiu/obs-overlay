#ifndef X11_OVERLAY_H
#define X11_OVERLAY_H

typedef struct OverlayWindow OverlayWindow;

OverlayWindow* CreateOverlayWindow(unsigned long mainWindow, int x, int y, int width, int height);
void UpdateOverlayPosition(OverlayWindow* overlay, int x, int y);
void UpdateOverlaySize(OverlayWindow* overlay, int width, int height);
void ShowOverlay(OverlayWindow* overlay);
void HideOverlay(OverlayWindow* overlay);
void DestroyOverlayWindow(OverlayWindow* overlay);

#endif
