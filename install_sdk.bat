@echo off
echo ========================================
echo Android SDK 快速安装指南
echo ========================================
echo.
echo 由于权限限制，需要手动完成以下步骤：
echo.
echo 1. 下载 Android Command Line Tools
echo    https://dl.google.com/android/repository/commandlinetools-win-9477386_latest.zip
echo.
echo 2. 解压到任意目录（如 D:\Android\sdk）
echo.
echo 3. 设置环境变量：
echo    ANDROID_HOME=D:\Android\sdk
echo    PATH=D:\Android\sdk\cmdline-tools\latest\bin;%%PATH%%
echo.
echo 4. 安装必要的 SDK 包：
echo    sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
echo.
echo 5. 创建 local.properties 文件：
echo    sdk.dir=D\:\\Android\\sdk
echo.
echo 6. 编译 APK：
echo    cd android
echo    gradlew.bat assembleDebug
echo.
echo ========================================
echo 或者使用 GitHub Actions 自动编译（无需安装 SDK）
echo ========================================
echo.
echo 1. 创建 GitHub 仓库
echo 2. 推送代码
echo 3. 自动下载 APK
echo.
pause
