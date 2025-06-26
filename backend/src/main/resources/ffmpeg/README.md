# FFmpeg 二进制文件配置说明

## 目录结构

```
src/main/resources/ffmpeg/
├── windows/          # Windows 平台二进制文件
│   ├── ffmpeg.exe
│   └── ffprobe.exe
├── linux/            # Linux 平台二进制文件
│   ├── ffmpeg
│   └── ffprobe
├── macos/            # macOS 平台二进制文件
│   ├── ffmpeg
│   └── ffprobe
└── README.md         # 本说明文件
```

## 下载和配置说明

### Windows 平台

1. 访问 [FFmpeg 官方下载页面](https://ffmpeg.org/download.html#build-windows)
2. 下载 Windows 版本的 FFmpeg 静态构建版本
3. 解压下载的文件
4. 将 `ffmpeg.exe` 和 `ffprobe.exe` 复制到 `src/main/resources/ffmpeg/windows/` 目录

### Linux 平台

1. 下载 Linux 静态构建版本：

   ```bash
   # 64位 Linux
   wget https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-amd64-static.tar.xz
   tar -xf ffmpeg-release-amd64-static.tar.xz
   ```

2. 将 `ffmpeg` 和 `ffprobe` 二进制文件复制到 `src/main/resources/ffmpeg/linux/` 目录

### macOS 平台

1. 下载 macOS 静态构建版本：

   ```bash
   # 使用 Homebrew 安装后复制
   brew install ffmpeg
   cp /usr/local/bin/ffmpeg src/main/resources/ffmpeg/macos/
   cp /usr/local/bin/ffprobe src/main/resources/ffmpeg/macos/
   ```

2. 或者从 [Evermeet.cx](https://evermeet.cx/ffmpeg/) 下载静态版本

## 系统 FFmpeg 优先级

应用程序会按以下优先级查找 FFmpeg：

1. **系统 PATH** - 如果系统已安装 FFmpeg 并可在 PATH 中找到，将优先使用
2. **内置二进制文件** - 使用项目中打包的对应平台版本
3. **错误提示** - 如果都不可用，会生成详细的安装说明文件

## 开发建议

### 推荐方案：使用系统 FFmpeg

对于开发环境，推荐直接在系统中安装 FFmpeg：

```bash
# Windows (使用 chocolatey)
choco install ffmpeg

# macOS (使用 Homebrew)
brew install ffmpeg

# Ubuntu/Debian
sudo apt-get install ffmpeg

# CentOS/RHEL
sudo yum install ffmpeg
```

### 生产部署：内置二进制文件

对于生产环境部署，建议将对应平台的 FFmpeg 二进制文件打包到项目中，确保：

- 跨环境一致性
- 版本控制
- 减少外部依赖

## 许可证注意事项

请注意 FFmpeg 的许可证要求：

- FFmpeg 使用 LGPL 许可证
- 如果您的项目是商业项目，请确保遵守相关许可证条款
- 建议在项目文档中注明使用了 FFmpeg

## 文件大小考虑

FFmpeg 二进制文件较大（通常 50-100MB），考虑：

- 使用 Git LFS 管理大型二进制文件
- 或者在 CI/CD 过程中动态下载
- 只包含必要平台的版本

## 验证安装

应用启动后，查看日志输出：

```
2024-01-01 10:00:00 [main] INFO  c.v.config.FFmpegConfig - 检测到系统平台: windows 10 (windows)
2024-01-01 10:00:00 [main] INFO  c.v.config.FFmpegConfig - 使用系统 PATH 中的 FFmpeg
```

或者调用测试接口验证：

```
GET /api/video/status
```
