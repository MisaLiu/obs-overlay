# Building Native Libraries

## Linux

### Prerequisites
- GCC compiler
- CMake 3.10+
- Make
- X11 development libraries
- OpenGL development libraries

### Manual Build
```bash
cd native/linux
./build.sh
```

### Gradle Integration
The Linux native library is automatically built when running Gradle on Linux:
```bash
./gradlew build
```

The build task will compile `libglx_hook.so` and copy it to `common/src/main/resources/lib/glx_hook.x64.so`.

## Windows

Windows native libraries (MinHook) are pre-compiled and included in the repository.

## Verification

To verify native libraries are included in the JAR:
```bash
unzip -l fabric/build/libs/obs_overlay-fabric-*.jar | grep "lib/"
```

Expected output:
- `lib/MinHook.x64.dll` (Windows x64)
- `lib/MinHook.x86.dll` (Windows x86)
- `lib/glx_hook.x64.so` (Linux x64)
