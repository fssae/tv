# Android TV 视频投屏接收器

基于 Golang + Android NDK 的局域网视频投屏方案，支持手机通过 Wi-Fi 上传视频到电视并自动播放。

## 项目结构

```
MP3toTV/
├── server/                    # Golang Web 服务器
│   ├── main.go               # 服务器主程序
│   └── go.mod                # Go 模块配置
├── android/                   # Android 应用
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/example/tvreceiver/
│   │   │   │   ├── MainActivity.java              # 主界面
│   │   │   │   ├── GoServerManager.java           # Go 进程管理
│   │   │   │   ├── VideoFileObserver.java        # 文件监听
│   │   │   │   ├── VideoPlayerManager.java       # 视频播放
│   │   │   │   ├── ServerService.java            # 后台服务
│   │   │   │   └── BootReceiver.java             # 开机自启
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   └── activity_main.xml
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   └── themes.xml
│   │   │   │   └── drawable/
│   │   │   │       └── ip_card_bg.xml
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle
│   ├── build.gradle
│   └── settings.gradle
├── build.sh                   # Linux/macOS 编译脚本
├── build.bat                  # Windows 编译脚本
└── README.md                  # 本文档
```

## 核心功能

### Golang 服务器
- HTTP 文件上传接口（支持 multipart/form-data）
- 内置美观的 HTML 上传页面
- 自动获取局域网 IP 地址
- 流式写入视频文件，避免内存溢出
- 自动端口检测（8080-8100）

### Android 应用
- 自动部署和启动 Go 二进制文件
- FileObserver 监听视频文件变化
- ExoPlayer 全屏播放
- 开机自启支持
- 进程守护和自动重连
- Android TV 适配（Leanback）

## 编译步骤

### 1. 编译 Go 二进制文件

#### Windows
```bash
build.bat
```

#### Linux/macOS
```bash
chmod +x build.sh
./build.sh
```

#### 手动编译
```bash
# ARM64 (主流设备)
CGO_ENABLED=0 GOOS=android GOARCH=arm64 go build -ldflags="-s -w" -o android/app/src/main/assets/server/libserver.so ./server

# ARM32 (旧设备)
CGO_ENABLED=0 GOOS=android GOARCH=arm GOARM=7 go build -ldflags="-s -w" -o android/app/src/main/assets/server/libserver_arm32.so ./server

# x86_64 (模拟器)
CGO_ENABLED=0 GOOS=android GOARCH=amd64 go build -ldflags="-s -w" -o android/app/src/main/assets/server/libserver_x64.so ./server
```

### 2. 编译 Android APK

```bash
cd android
./gradlew assembleDebug
```

生成的 APK 位于 `android/app/build/outputs/apk/debug/app-debug.apk`

## 使用方法

### 1. 安装应用
将 APK 安装到 Android TV 设备上

### 2. 启动应用
- 应用会自动启动 Go 服务器
- 屏幕显示电视的局域网 IP 和端口（如 `192.168.1.100:8080`）

### 3. 手机上传视频
- 确保手机和电视在同一 Wi-Fi 网络
- 在手机浏览器访问电视显示的 IP 地址
- 选择视频文件上传
- 电视会自动开始播放

## 技术要点

### 1. Go 进程管理
- 二进制文件从 assets 拷贝到内部存储
- 使用 `Runtime.getRuntime().exec()` 启动进程
- 自动设置执行权限（chmod 755）
- 进程状态监控和自动重启

### 2. 文件监听
- 使用 `FileObserver` 监听 `CLOSE_WRITE` 事件
- 避免文件未完全写入就触发播放
- 监听 Android 私有目录，规避 Scoped Storage

### 3. 视频播放
- 使用 ExoPlayer 支持多种视频格式
- 全屏播放，自动隐藏控制栏
- 播放完成自动返回主界面

### 4. 权限处理
- INTERNET：网络通信
- ACCESS_WIFI_STATE：获取本机 IP
- WAKE_LOCK：保持屏幕常亮
- RECEIVE_BOOT_COMPLETED：开机自启

### 5. Android TV 适配
- 声明 `android.software.leanback` 特性
- 横屏显示
- 使用 TV 主题和布局

## 注意事项

1. **网络环境**：手机和电视必须在同一局域网
2. **防火墙**：确保电视的 8080 端口未被防火墙阻止
3. **视频格式**：支持 MP4、MKV、AVI、MOV 等常见格式
4. **存储空间**：确保电视有足够的存储空间
5. **Go 版本**：需要 Go 1.21 或更高版本

## 故障排查

### 服务器无法启动
- 检查 Go 二进制文件是否正确编译
- 查看日志确认二进制文件路径
- 确认执行权限设置成功

### 无法访问上传页面
- 检查电视和手机是否在同一网络
- 确认防火墙设置
- 查看电视显示的 IP 是否正确

### 视频无法播放
- 检查视频文件格式是否支持
- 确认文件已完全上传
- 查看播放器日志

## 性能优化

1. **Go 二进制优化**：使用 `-ldflags="-s -w"` 减小体积
2. **流式传输**：边上传边写入，避免内存占用过高
3. **文件监听**：使用 FileObserver 替代轮询
4. **播放器缓存**：ExoPlayer 自带缓冲机制

## 安全建议

1. 仅在可信局域网使用
2. 考虑添加简单的访问控制
3. 限制上传文件大小
4. 验证文件类型

## 扩展功能

- [ ] 添加用户认证
- [ ] 支持播放列表
- [ ] 视频预览功能
- [ ] 下载历史记录
- [ ] 局域网设备发现（mDNS）
