package com.video.service.impl;

import com.video.config.FFmpegConfig;
import com.video.service.AudioExtractionService;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 音频提取服务实现
 * 使用FFmpeg进行音频提取
 */
@Service
public class AudioExtractionServiceImpl implements AudioExtractionService {

    private static final Logger log = LoggerFactory.getLogger(AudioExtractionServiceImpl.class);

    @Autowired
    private FFmpegConfig ffmpegConfig;

    @Value("${file.temp-path:./temp/}")
    private String tempPath;

    private FFmpeg ffmpeg;
    private FFprobe ffprobe;

    /**
     * 初始化FFmpeg和FFprobe
     */
    private void initFFmpeg() throws IOException {
        if (ffmpeg == null || ffprobe == null) {
            String ffmpegPath = ffmpegConfig.getFFmpegPath();
            String ffprobePath = ffmpegConfig.getFFprobePath();

            if (ffmpegPath == null || ffprobePath == null) {
                throw new RuntimeException("FFmpeg 配置未初始化，请检查 FFmpeg 安装");
            }

            ffmpeg = new FFmpeg(ffmpegPath);
            ffprobe = new FFprobe(ffprobePath);
            log.info("FFmpeg初始化成功: {}", ffmpegPath);
        }
    }

    @Override
    public String extractAudio(String videoPath) {
        try {
            log.info("开始从视频提取音频: {}", videoPath);

            // 检查 FFmpeg 是否可用
            if (!ffmpegConfig.isFFmpegAvailable()) {
                String errorMsg = "FFmpeg 不可用，请安装 FFmpeg 或检查配置。请查看日志中的安装说明。";
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            // 验证视频文件
            if (!validateVideoFile(videoPath)) {
                String errorMsg = "视频文件验证失败，文件可能损坏或格式不支持: " + videoPath;
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            // 初始化FFmpeg
            initFFmpeg();

            // 创建临时目录
            Path tempDir = Paths.get(tempPath);
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }

            // 生成音频文件路径
            String videoFileName = Paths.get(videoPath).getFileName().toString();
            String audioFileName = videoFileName.replaceAll("\\.[^.]+$", "_extracted.wav");
            Path audioPath = tempDir.resolve(audioFileName);

            // 构建FFmpeg命令
            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(videoPath)
                    .overrideOutputFiles(true)
                    .addOutput(audioPath.toString())
                    .setFormat("wav") // 输出WAV格式
                    .setAudioChannels(1) // 单声道
                    .setAudioSampleRate(16000) // 16kHz采样率，适合语音识别
                    .setAudioBitRate(128_000) // 128kbps比特率
                    .done();

            // 执行音频提取
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

            try {
                executor.createJob(builder).run();
                log.info("音频提取成功: {}", audioPath.toString());
                return audioPath.toString();
            } catch (RuntimeException e) {
                // 增强错误信息，显示FFmpeg的详细输出
                String errorMsg = "FFmpeg执行失败: " + e.getMessage();
                log.error("FFmpeg命令执行失败，输入视频: {}, 输出音频: {}, 错误: {}",
                        videoPath, audioPath.toString(), errorMsg);

                // 尝试手动执行FFmpeg命令来获取更详细的错误信息
                try {
                    ProcessBuilder pb = new ProcessBuilder(
                            ffmpegConfig.getFFmpegPath(),
                            "-i", videoPath,
                            "-f", "wav",
                            "-ac", "1",
                            "-ar", "16000",
                            "-ab", "128k",
                            "-y", // 覆盖输出文件
                            audioPath.toString());
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
                    log.error("FFmpeg详细输出 (退出码: {}): \n{}", exitCode, output.toString());

                } catch (Exception debugException) {
                    log.error("获取FFmpeg详细错误信息失败: {}", debugException.getMessage());
                }

                throw e;
            }

        } catch (Exception e) {
            log.error("音频提取失败: {}", videoPath, e);
            throw new RuntimeException("音频提取失败: " + e.getMessage(), e);
        }
    }

    @Override
    public AudioInfo getAudioInfo(String audioPath) {
        try {
            // 检查 FFmpeg 是否可用
            if (!ffmpegConfig.isFFmpegAvailable()) {
                String errorMsg = "FFmpeg 不可用，无法获取音频信息";
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            // 初始化FFprobe
            initFFmpeg();

            try {
                // 获取音频文件信息
                FFmpegProbeResult probeResult = ffprobe.probe(audioPath);

                // 查找音频流
                FFmpegStream audioStream = probeResult.getStreams().stream()
                        .filter(stream -> stream.codec_type == FFmpegStream.CodecType.AUDIO)
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("未找到音频流"));

                // 提取音频信息
                double duration = probeResult.getFormat().duration;
                int sampleRate = audioStream.sample_rate;
                int channels = audioStream.channels;

                log.info("音频信息: 时长={}秒, 采样率={}Hz, 声道数={}", duration, sampleRate, channels);

                return new AudioInfo(duration, sampleRate, channels);

            } catch (RuntimeException e) {
                // 如果 FFprobe 失败，手动执行获取详细错误信息
                log.warn("FFprobe 库调用失败，尝试手动执行获取详细错误: {}", e.getMessage());

                try {
                    ProcessBuilder pb = new ProcessBuilder(
                            ffmpegConfig.getFFprobePath(),
                            "-v", "error",
                            "-show_entries", "format=duration",
                            "-show_entries", "stream=codec_type,sample_rate,channels",
                            "-of", "csv=p=0",
                            audioPath);
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
                    String outputStr = output.toString().trim();

                    if (exitCode != 0) {
                        log.error("FFprobe 手动执行失败 (退出码: {}): \n{}", exitCode, outputStr);

                        // 分析具体错误
                        String detailedError = analyzeFFprobeError(outputStr);
                        throw new RuntimeException("视频文件格式错误: " + detailedError);
                    } else {
                        log.info("FFprobe 手动执行成功: {}", outputStr);
                        // 如果手动执行成功，返回基本信息
                        return new AudioInfo(0.0, 16000, 1); // 默认值
                    }

                } catch (Exception manualException) {
                    log.error("FFprobe 手动执行也失败: {}", manualException.getMessage());
                    throw new RuntimeException("获取音频信息失败: " + e.getMessage() +
                            " (手动检查也失败: " + manualException.getMessage() + ")");
                }
            }

        } catch (Exception e) {
            log.error("获取音频信息失败: {}", audioPath, e);
            throw new RuntimeException("获取音频信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查FFmpeg是否可用
     */
    public boolean isFFmpegAvailable() {
        return ffmpegConfig.isFFmpegAvailable();
    }

    /**
     * 分析 FFprobe 错误信息
     */
    private String analyzeFFprobeError(String errorOutput) {
        if (errorOutput == null || errorOutput.isEmpty()) {
            return "未知错误";
        }

        String lowerError = errorOutput.toLowerCase();

        if (lowerError.contains("moov atom not found")) {
            return "MP4文件缺少moov原子（元数据），文件可能损坏或上传不完整";
        } else if (lowerError.contains("invalid data found")) {
            return "视频文件包含无效数据，文件已损坏";
        } else if (lowerError.contains("no such file")) {
            return "视频文件不存在或路径无效";
        } else if (lowerError.contains("permission denied")) {
            return "没有权限访问视频文件";
        } else if (lowerError.contains("format not detected") || lowerError.contains("unknown format")) {
            return "视频文件格式无法识别，可能不是有效的视频文件";
        } else if (lowerError.contains("truncated")) {
            return "视频文件被截断，上传可能不完整";
        } else if (lowerError.contains("codec not found")) {
            return "视频文件使用了不支持的编码格式";
        } else if (lowerError.contains("header missing") || lowerError.contains("invalid header")) {
            return "视频文件头部信息缺失或损坏";
        } else {
            // 返回原始错误信息的前100个字符
            return errorOutput.length() > 100 ? errorOutput.substring(0, 100) + "..." : errorOutput;
        }
    }

    /**
     * 验证视频文件是否有效
     */
    private boolean validateVideoFile(String videoPath) {
        try {
            log.info("验证视频文件: {}", videoPath);

            // 检查文件是否存在
            Path file = Paths.get(videoPath);
            if (!Files.exists(file)) {
                log.error("视频文件不存在: {}", videoPath);
                return false;
            }

            // 检查文件大小
            long fileSize = Files.size(file);
            if (fileSize == 0) {
                log.error("视频文件为空: {}", videoPath);
                return false;
            }

            log.info("文件基本检查通过: {} (大小: {}字节)", videoPath, fileSize);

            // 使用FFprobe检查视频文件结构
            try {
                initFFmpeg();
                FFmpegProbeResult probeResult = ffprobe.probe(videoPath);

                // 检查是否有视频流
                boolean hasVideoStream = probeResult.getStreams().stream()
                        .anyMatch(stream -> stream.codec_type == FFmpegStream.CodecType.VIDEO);

                if (!hasVideoStream) {
                    log.error("视频文件中未找到视频流: {}", videoPath);
                    return false;
                }

                // 检查文件时长
                double duration = probeResult.getFormat().duration;
                if (duration <= 0) {
                    log.warn("视频文件时长异常: {} (时长: {}秒)", videoPath, duration);
                }

                log.info("视频文件验证通过: {} (时长: {}秒)", videoPath, duration);
                return true;

            } catch (Exception e) {
                log.error("视频文件结构验证失败: {} - {}", videoPath, e.getMessage());
                return false;
            }

        } catch (Exception e) {
            log.error("视频文件验证过程出错: {} - {}", videoPath, e.getMessage());
            return false;
        }
    }
}