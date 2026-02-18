# Build script for all Android architectures
$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ServerDir = Join-Path $ScriptDir "server"
$AndroidAssetsDir = Join-Path $ScriptDir "android\app\src\main\assets\server"

Write-Host "Building Go server for all Android architectures..."

if (-not (Test-Path $AndroidAssetsDir)) {
    New-Item -ItemType Directory -Path $AndroidAssetsDir | Out-Null
}

Set-Location $ServerDir

# ARM64
Write-Host "Building for arm64-v8a..."
$env:CGO_ENABLED = "0"
$env:GOOS = "android"
$env:GOARCH = "arm64"
go build -o "$AndroidAssetsDir\libserver_arm64.so" .
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ ARM64 build successful"
} else {
    Write-Host "✗ ARM64 build failed"
}

# ARM32
Write-Host "Building for armeabi-v7a..."
$env:GOARCH = "arm"
$env:GOARM = "7"
go build -o "$AndroidAssetsDir\libserver_arm32.so" .
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ ARM32 build successful"
} else {
    Write-Host "✗ ARM32 build failed"
}

# x86_64
Write-Host "Building for x86_64..."
$env:GOARCH = "amd64"
go build -o "$AndroidAssetsDir\libserver_x64.so" .
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ x86_64 build successful"
} else {
    Write-Host "✗ x86_64 build failed"
}

Write-Host "`nBuild completed!"
Write-Host "Binaries located at: $AndroidAssetsDir"
Get-ChildItem $AndroidAssetsDir | Format-Table Name, Length
