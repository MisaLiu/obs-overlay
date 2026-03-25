#!/bin/bash
set -e

cd "$(dirname "$0")"

rm -rf build
mkdir -p build
cd build

cmake ..
make

RESOURCE_DIR="../../../common/src/main/resources/lib"
mkdir -p "$RESOURCE_DIR"
cp libglx_hook.so "$RESOURCE_DIR/glx_hook.x64.so"

echo "Build complete: glx_hook.x64.so copied to resources"
