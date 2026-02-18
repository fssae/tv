@echo off
setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set SERVER_DIR=%SCRIPT_DIR%server
set ANDROID_ASSETS_DIR=%SCRIPT_DIR%android\app\src\main\assets\server

echo Building Go server for Android...

if not exist "%ANDROID_ASSETS_DIR%" mkdir "%ANDROID_ASSETS_DIR%"

cd /d "%SERVER_DIR%"

echo Building for arm64-v8a...
set CGO_ENABLED=0
set GOOS=android
set GOARCH=arm64
go build -ldflags="-s -w" -o "%ANDROID_ASSETS_DIR%\libserver.so" .

echo Building for armeabi-v7a...
set GOARCH=arm
set GOARM=7
go build -ldflags="-s -w" -o "%ANDROID_ASSETS_DIR%\libserver_arm32.so" .

echo Building for x86_64 (emulator)...
set GOARCH=amd64
go build -ldflags="-s -w" -o "%ANDROID_ASSETS_DIR%\libserver_x64.so" .

echo Build completed successfully!
echo Binaries are located at: %ANDROID_ASSETS_DIR%
dir "%ANDROID_ASSETS_DIR%"

endlocal
