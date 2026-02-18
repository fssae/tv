$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ServerDir = Join-Path $ScriptDir "server"
$AndroidAssetsDir = Join-Path $ScriptDir "android\app\src\main\assets\server"

Write-Host "Building Go server for Android..."

if (-not (Test-Path $AndroidAssetsDir)) {
    New-Item -ItemType Directory -Path $AndroidAssetsDir | Out-Null
}

Set-Location $ServerDir

Write-Host "Building for arm64-v8a..."
$env:CGO_ENABLED = "0"
$env:GOOS = "android"
$env:GOARCH = "arm64"
go build -ldflags "-s -w" -o "$AndroidAssetsDir\libserver_arm64.so" .

Write-Host "Building for armeabi-v7a..."
$env:GOARCH = "arm"
$env:GOARM = "7"
go build -ldflags "-s -w" -o "$AndroidAssetsDir\libserver_arm32.so" .

Write-Host "Building for x86_64 (emulator)..."
$env:GOARCH = "amd64"
go build -ldflags "-s -w" -o "$AndroidAssetsDir\libserver_x64.so" .

Write-Host "Build completed successfully!"
Write-Host "Binaries are located at: $AndroidAssetsDir"
Get-ChildItem $AndroidAssetsDir
