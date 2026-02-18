#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_DIR="${SCRIPT_DIR}/server"
ANDROID_ASSETS_DIR="${SCRIPT_DIR}/android/app/src/main/assets/server"

echo "Building Go server for Android..."

mkdir -p "${ANDROID_ASSETS_DIR}"

echo "Building for arm64-v8a..."
cd "${SERVER_DIR}"
CGO_ENABLED=0 GOOS=android GOARCH=arm64 go build -ldflags="-s -w" -o "${ANDROID_ASSETS_DIR}/libserver.so" .

echo "Building for armeabi-v7a..."
CGO_ENABLED=0 GOOS=android GOARCH=arm GOARM=7 go build -ldflags="-s -w" -o "${ANDROID_ASSETS_DIR}/libserver_arm32.so" .

echo "Building for x86_64 (emulator)..."
CGO_ENABLED=0 GOOS=android GOARCH=amd64 go build -ldflags="-s -w" -o "${ANDROID_ASSETS_DIR}/libserver_x64.so" .

echo "Build completed successfully!"
echo "Binaries are located at: ${ANDROID_ASSETS_DIR}"
ls -la "${ANDROID_ASSETS_DIR}"
