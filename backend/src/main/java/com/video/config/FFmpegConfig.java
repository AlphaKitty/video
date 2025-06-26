package com.video.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * FFmpeg 配置类
 * 支持跨平台自动检测和二进制文件管理
 */
@Configuration
public class FFmpegConfig {

    private static final Logger log = LoggerFactory.getLogger(FFmpegConfig.class);

    @Value("${file.temp-path:./temp/}")
    private String tempPath;

    private String ffmpegPath;
    private String ffprobePath;

    @PostConstruct
    public void init() {
        try {
            initFFmpegPaths();
        } catch (Exception e) {
            log.error("FFmpeg 初始化失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 初始化 FFmpeg 路径
     */
    private void initFFmpegPaths() throws IOException {
        String osName = System.getProperty("os.name").toLowerCase();
        String platform = detectPlatform(osName);

        log.info("检测到系统平台: {} ({})", osName, platform);

        // 首先尝试使用系统 PATH 中的 FFmpeg
        if (isSystemFFmpegAvailable()) {
            ffmpegPath = "ffmpeg";
            ffprobePath = "ffprobe";
            log.info("使用系统 PATH 中的 FFmpeg");
            return;
        }

        // 系统 FFmpeg 不可用，使用内置版本
        log.info("系统 FFmpeg 不可用，尝试使用内置版本");
        extractAndSetupFFmpeg(platform);
    }

    /**
     * 检测系统平台
     */
    private String detectPlatform(String osName) {
        if (osName.contains("win")) {
            return "windows";
        } else if (osName.contains("mac")) {
            return "macos";
        } else if (osName.contains("nix") || osName.contains("nux")) {
            return "linux";
        } else {
            log.warn("未知平台: {}, 默认使用 linux", osName);
            return "linux";
        }
    }

    /**
     * 检查系统 PATH 中是否有可用的 FFmpeg
     */
    private boolean isSystemFFmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("系统 FFmpeg 可用");
                return true;
            }
        } catch (Exception e) {
            log.debug("系统 FFmpeg 不可用: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 提取并设置内置 FFmpeg
     */
    private void extractAndSetupFFmpeg(String platform) throws IOException {
        // 创建临时目录
        Path tempDir = Paths.get(tempPath, "ffmpeg");
        Files.createDirectories(tempDir);

        String ffmpegName = platform.equals("windows") ? "ffmpeg.exe" : "ffmpeg";
        String ffprobeName = platform.equals("windows") ? "ffprobe.exe" : "ffprobe";

        try {
            // 尝试从资源目录提取
            Path ffmpegFile = extractBinaryFromResources(platform, ffmpegName, tempDir);
            Path ffprobeFile = extractBinaryFromResources(platform, ffprobeName, tempDir);

            if (ffmpegFile != null && ffprobeFile != null) {
                // 设置执行权限（Linux/Mac）
                if (!platform.equals("windows")) {
                    setExecutable(ffmpegFile);
                    setExecutable(ffprobeFile);
                }

                ffmpegPath = ffmpegFile.toString();
                ffprobePath = ffprobeFile.toString();
                log.info("成功设置内置 FFmpeg: {}", ffmpegPath);
                return;
            }
        } catch (Exception e) {
            log.warn("提取内置 FFmpeg 失败: {}", e.getMessage());
        }

        // 如果内置版本不可用，创建下载说明文件
        createFFmpegDownloadInstructions(platform, tempDir);

        // 设置默认路径（可能会失败，但提供明确的错误信息）
        ffmpegPath = tempDir.resolve(ffmpegName).toString();
        ffprobePath = tempDir.resolve(ffprobeName).toString();
    }

    /**
     * 从资源目录提取二进制文件
     */
    private Path extractBinaryFromResources(String platform, String fileName, Path targetDir) throws IOException {
        String resourcePath = "ffmpeg/" + platform + "/" + fileName;

        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                log.info("资源文件不存在: {}", resourcePath);
                return null;
            }

            Path targetFile = targetDir.resolve(fileName);
            try (InputStream inputStream = resource.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
                log.info("成功提取二进制文件: {}", targetFile);
                return targetFile;
            }
        } catch (Exception e) {
            log.debug("提取二进制文件失败: {} - {}", resourcePath, e.getMessage());
            return null;
        }
    }

    /**
     * 设置文件为可执行
     */
    private void setExecutable(Path file) {
        try {
            File f = file.toFile();
            f.setExecutable(true);
            log.debug("设置文件为可执行: {}", file);
        } catch (Exception e) {
            log.warn("设置文件执行权限失败: {} - {}", file, e.getMessage());
        }
    }

    /**
     * 创建 FFmpeg 下载说明文件
     */
    private void createFFmpegDownloadInstructions(String platform, Path tempDir) {
        try {
            String instructions = generateDownloadInstructions(platform);
            Path instructionFile = tempDir.resolve("FFMPEG_SETUP_INSTRUCTIONS.txt");
            Files.write(instructionFile, instructions.getBytes());
            log.info("已创建 FFmpeg 安装说明文件: {}", instructionFile);
        } catch (Exception e) {
            log.warn("创建安装说明文件失败: {}", e.getMessage());
        }
    }

    /**
     * 生成平台相关的下载说明
     */
    private String generateDownloadInstructions(String platform) {
        StringBuilder sb = new StringBuilder();
        sb.append("FFmpeg 安装说明\n");
        sb.append("===============\n\n");
        sb.append("由于项目中未包含 ").append(platform).append(" 平台的 FFmpeg 二进制文件，\n");
        sb.append("请按照以下步骤安装 FFmpeg：\n\n");

        switch (platform) {
            case "windows":
                sb.append("Windows 安装方法：\n");
                sb.append("1. 下载 FFmpeg: https://ffmpeg.org/download.html#build-windows\n");
                sb.append("2. 解压到任意目录\n");
                sb.append("3. 将 ffmpeg.exe 和 ffprobe.exe 复制到：\n");
                sb.append("   ").append(tempPath).append("ffmpeg/\n");
                sb.append("4. 或者将 FFmpeg 目录添加到系统 PATH 环境变量\n");
                break;
            case "linux":
                sb.append("Linux 安装方法：\n");
                sb.append("1. Ubuntu/Debian: sudo apt-get install ffmpeg\n");
                sb.append("2. CentOS/RHEL: sudo yum install ffmpeg 或 sudo dnf install ffmpeg\n");
                sb.append("3. 或者下载静态构建版本到：\n");
                sb.append("   ").append(tempPath).append("ffmpeg/\n");
                break;
            case "macos":
                sb.append("macOS 安装方法：\n");
                sb.append("1. 使用 Homebrew: brew install ffmpeg\n");
                sb.append("2. 或者下载静态构建版本到：\n");
                sb.append("   ").append(tempPath).append("ffmpeg/\n");
                break;
        }

        sb.append("\n安装完成后重启应用程序即可使用音频提取功能。\n");
        return sb.toString();
    }

    // Getters
    public String getFFmpegPath() {
        return ffmpegPath;
    }

    public String getFFprobePath() {
        return ffprobePath;
    }

    /**
     * 获取 FFmpeg 状态信息
     */
    public String getFFmpegStatus() {
        StringBuilder status = new StringBuilder();
        status.append("FFmpeg 配置状态:\n");
        status.append("- 系统平台: ").append(System.getProperty("os.name")).append("\n");
        status.append("- FFmpeg路径: ").append(ffmpegPath).append("\n");
        status.append("- FFprobe路径: ").append(ffprobePath).append("\n");

        // 检查文件是否存在
        if (ffmpegPath != null) {
            java.io.File ffmpegFile = new java.io.File(ffmpegPath);
            status.append("- FFmpeg文件存在: ").append(ffmpegFile.exists()).append("\n");
            if (ffmpegFile.exists()) {
                status.append("- FFmpeg文件大小: ").append(ffmpegFile.length()).append(" bytes\n");
                status.append("- FFmpeg可执行: ").append(ffmpegFile.canExecute()).append("\n");
            }
        }

        status.append("- FFmpeg可用性: ").append(isFFmpegAvailable()).append("\n");
        return status.toString();
    }

    /**
     * 检查 FFmpeg 是否可用
     */
    public boolean isFFmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(ffmpegPath, "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取输出
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("FFmpeg可用性检查通过，版本信息: {}",
                        output.toString().split("\n")[0]); // 只显示第一行版本信息
                return true;
            } else {
                log.error("FFmpeg可用性检查失败 (退出码: {}): \n{}", exitCode, output.toString());
                return false;
            }
        } catch (Exception e) {
            log.error("FFmpeg可用性检查异常: {}", e.getMessage(), e);
            return false;
        }
    }
}