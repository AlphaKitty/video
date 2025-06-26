package com.video.controller;

import com.video.common.Result;
import com.video.config.FFmpegConfig;
import com.video.model.VideoTask;
import com.video.service.AudioExtractionService;
import com.video.service.VideoService;
import com.video.service.WhisperService;
import com.video.service.TranslationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 视频处理控制器
 */
@RestController
@RequestMapping("/api/video")
@CrossOrigin(origins = "*")
public class VideoController {

    private static final Logger log = LoggerFactory.getLogger(VideoController.class);

    private final VideoService videoService;
    private final AudioExtractionService audioExtractionService;
    private final WhisperService whisperService;
    private final FFmpegConfig ffmpegConfig;
    private final TranslationService translationService;

    public VideoController(VideoService videoService, AudioExtractionService audioExtractionService,
            WhisperService whisperService, FFmpegConfig ffmpegConfig, TranslationService translationService) {
        this.videoService = videoService;
        this.audioExtractionService = audioExtractionService;
        this.whisperService = whisperService;
        this.ffmpegConfig = ffmpegConfig;
        this.translationService = translationService;
    }

    /**
     * 上传视频文件
     */
    @PostMapping("/upload")
    public Result<VideoTask> uploadVideo(@RequestParam("file") MultipartFile file) {
        try {
            log.info("开始上传视频文件: {}", file.getOriginalFilename());
            VideoTask task = videoService.uploadVideo(file);

            // 检查任务状态，如果上传失败则返回详细错误信息
            if ("UPLOAD_FAILED".equals(task.getStatus())) {
                log.error("视频文件验证失败: {}", task.getErrorMessage());
                return Result.error("❌ " + task.getErrorMessage() +
                        "\n\n💡 建议：\n" +
                        "1. 检查视频文件是否完整\n" +
                        "2. 重新下载或重新录制视频\n" +
                        "3. 尝试使用其他格式的视频文件\n" +
                        "4. 确保文件在传输过程中没有损坏");
            }

            return Result.success(task);
        } catch (Exception e) {
            log.error("视频上传失败", e);

            // 根据错误类型提供不同的建议
            String errorMsg = e.getMessage();
            String suggestion = "";

            if (errorMsg.contains("MP4文件损坏") || errorMsg.contains("moov atom")) {
                suggestion = "\n\n💡 建议：\n" +
                        "• 这是MP4文件结构问题，通常由以下原因造成：\n" +
                        "  1. 文件上传过程中断\n" +
                        "  2. 视频录制时异常结束\n" +
                        "  3. 文件传输损坏\n" +
                        "• 解决方法：重新获取完整的视频文件后再上传";
            } else if (errorMsg.contains("数据损坏") || errorMsg.contains("Invalid data")) {
                suggestion = "\n\n💡 建议：\n" +
                        "• 视频文件内容已损坏\n" +
                        "• 请检查原始文件是否正常播放\n" +
                        "• 尝试重新获取或重新生成视频文件";
            } else if (errorMsg.contains("FFmpeg不可用")) {
                suggestion = "\n\n💡 建议：\n" +
                        "• 系统FFmpeg组件未正确安装\n" +
                        "• 请联系管理员检查服务器配置";
            }

            return Result.error("❌ 视频上传失败: " + errorMsg + suggestion);
        }
    }

    /**
     * 开始处理视频
     */
    @PostMapping("/process/{taskId}")
    public Result<Void> processVideo(@PathVariable Long taskId) {
        try {
            log.info("开始处理视频任务: {}", taskId);

            // 先检查任务状态
            VideoTask task = videoService.getTask(taskId);
            if ("UPLOAD_FAILED".equals(task.getStatus())) {
                return Result.error("❌ 无法处理失败的任务: " + task.getErrorMessage() +
                        "\n\n请重新上传有效的视频文件");
            }

            videoService.processVideo(taskId);
            return Result.success();
        } catch (Exception e) {
            log.error("视频处理失败", e);

            String errorMsg = e.getMessage();
            String suggestion = "";

            if (errorMsg.contains("视频文件验证失败")) {
                suggestion = "\n\n💡 提示：\n" +
                        "• 视频文件在处理过程中被检测为无效\n" +
                        "• 可能的原因：文件损坏、格式不支持、上传不完整\n" +
                        "• 建议：重新上传完整的视频文件";
            }

            return Result.error("❌ 视频处理失败: " + errorMsg + suggestion);
        }
    }

    /**
     * 获取任务状态
     */
    @GetMapping("/task/{taskId}")
    public Result<VideoTask> getTask(@PathVariable Long taskId) {
        try {
            VideoTask task = videoService.getTask(taskId);
            return Result.success(task);
        } catch (Exception e) {
            log.error("获取任务状态失败", e);
            return Result.error("获取任务状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务列表
     */
    @GetMapping("/tasks")
    public Result<List<VideoTask>> getTasks() {
        try {
            List<VideoTask> tasks = videoService.getAllTasks();
            return Result.success(tasks);
        } catch (Exception e) {
            log.error("获取任务列表失败", e);
            return Result.error("获取任务列表失败: " + e.getMessage());
        }
    }

    /**
     * 更新字幕
     */
    @PostMapping("/subtitle/{taskId}")
    public Result<Void> updateSubtitle(@PathVariable Long taskId, @RequestBody String subtitleContent) {
        try {
            log.info("更新字幕内容: {}", taskId);
            videoService.updateSubtitle(taskId, subtitleContent);
            return Result.success();
        } catch (Exception e) {
            log.error("更新字幕失败", e);
            return Result.error("更新字幕失败: " + e.getMessage());
        }
    }

    /**
     * 测试音频提取功能
     */
    @PostMapping("/test/extract-audio/{taskId}")
    public Result<String> testExtractAudio(@PathVariable Long taskId) {
        try {
            log.info("测试音频提取: {}", taskId);
            VideoTask task = videoService.getTask(taskId);

            // 提取音频
            String audioPath = audioExtractionService.extractAudio(task.getFilePath());

            // 获取音频信息
            AudioExtractionService.AudioInfo audioInfo = audioExtractionService.getAudioInfo(audioPath);

            String result = String.format(
                    "音频提取成功！\\n" +
                            "音频文件: %s\\n" +
                            "时长: %.2f秒\\n" +
                            "采样率: %dHz\\n" +
                            "声道数: %d",
                    audioPath, audioInfo.getDuration(), audioInfo.getSampleRate(), audioInfo.getChannels());

            return Result.success(result);
        } catch (Exception e) {
            log.error("测试音频提取失败", e);
            return Result.error("测试音频提取失败: " + e.getMessage());
        }
    }

    /**
     * 测试语音识别功能
     */
    @PostMapping("/test/transcribe/{taskId}")
    public Result<String> testTranscribe(@PathVariable Long taskId) {
        try {
            log.info("测试语音识别: {}", taskId);
            VideoTask task = videoService.getTask(taskId);

            // 执行语音识别
            String transcription = whisperService.transcribe(task.getFilePath());

            return Result.success(transcription);
        } catch (Exception e) {
            log.error("测试语音识别失败", e);
            return Result.error("测试语音识别失败: " + e.getMessage());
        }
    }

    /**
     * 快速检查 FFmpeg 状态
     */
    @GetMapping("/ffmpeg-status")
    public Result<String> getFFmpegStatus() {
        try {
            StringBuilder status = new StringBuilder();
            status.append("📊 FFmpeg 状态检查\\n\\n");

            // FFmpeg 可用性
            boolean available = ((com.video.service.impl.AudioExtractionServiceImpl) audioExtractionService)
                    .isFFmpegAvailable();
            status.append("✓ FFmpeg 可用: ").append(available ? "是" : "否").append("\\n");

            if (available) {
                status.append("✓ 系统已准备好处理视频文件\\n");
            } else {
                status.append("❌ FFmpeg 不可用，无法处理视频文件\\n");
                status.append("请检查 FFmpeg 安装或联系管理员\\n");
            }

            return Result.success(status.toString());
        } catch (Exception e) {
            return Result.error("检查 FFmpeg 状态失败: " + e.getMessage());
        }
    }

    /**
     * 验证视频文件
     */
    @PostMapping("/validate/{taskId}")
    public Result<String> validateVideoFile(@PathVariable Long taskId) {
        try {
            log.info("验证视频文件: {}", taskId);
            VideoTask task = videoService.getTask(taskId);

            // 基本文件信息
            java.io.File file = new java.io.File(task.getFilePath());
            StringBuilder result = new StringBuilder();
            result.append("视频文件验证结果:\\n");
            result.append("文件路径: ").append(task.getFilePath()).append("\\n");
            result.append("文件存在: ").append(file.exists()).append("\\n");
            if (file.exists()) {
                result.append("文件大小: ").append(file.length()).append(" 字节\\n");
                result.append("文件可读: ").append(file.canRead()).append("\\n");
            }

            // 使用FFprobe验证
            try {
                AudioExtractionService.AudioInfo audioInfo = audioExtractionService.getAudioInfo(task.getFilePath());
                result.append("FFprobe检查: 通过\\n");
                result.append("视频时长: ").append(audioInfo.getDuration()).append(" 秒\\n");
            } catch (Exception e) {
                result.append("FFprobe检查: 失败 - ").append(e.getMessage()).append("\\n");
            }

            return Result.success(result.toString());
        } catch (Exception e) {
            log.error("验证视频文件失败", e);
            return Result.error("验证视频文件失败: " + e.getMessage());
        }
    }

    /**
     * 检查服务状态
     */
    @GetMapping("/status")
    public Result<String> getServiceStatus() {
        try {
            StringBuilder status = new StringBuilder();
            status.append("服务状态检查:\\n\\n");

            // 详细的FFmpeg状态
            status.append(ffmpegConfig.getFFmpegStatus()).append("\\n");

            // 检查Whisper
            boolean whisperAvailable = ((com.video.service.impl.WhisperServiceImpl) whisperService)
                    .isWhisperAvailable();
            status.append("Whisper服务: ").append(whisperAvailable ? "可用" : "不可用").append("\\n");

            // 检查翻译服务
            status.append("\\n").append(((com.video.service.impl.TranslationServiceImpl) translationService)
                    .getServiceStatus()).append("\\n");

            // 任务统计
            List<VideoTask> tasks = videoService.getAllTasks();
            status.append("任务总数: ").append(tasks.size()).append("\\n");

            return Result.success(status.toString());
        } catch (Exception e) {
            log.error("获取服务状态失败", e);
            return Result.error("获取服务状态失败: " + e.getMessage());
        }
    }

    /**
     * 测试翻译功能
     */
    @PostMapping("/test/translate")
    public Result<String> testTranslate(@RequestParam("text") String text,
            @RequestParam(value = "fromLang", defaultValue = "zh") String fromLang,
            @RequestParam(value = "toLang", defaultValue = "vi") String toLang) {
        try {
            log.info("测试翻译: {} -> {}, 文本: {}", fromLang, toLang, text.substring(0, Math.min(50, text.length())));

            String translation = translationService.translate(text, fromLang, toLang);

            StringBuilder result = new StringBuilder();
            result.append("🌏 翻译测试结果:\\n\\n");
            result.append("原文 (").append(fromLang).append("): ").append(text).append("\\n\\n");
            result.append("译文 (").append(toLang).append("): ").append(translation).append("\\n\\n");
            result.append("服务状态: ").append(((com.video.service.impl.TranslationServiceImpl) translationService)
                    .isTranslationServiceAvailable() ? "API模式" : "模拟模式");

            return Result.success(result.toString());
        } catch (Exception e) {
            log.error("测试翻译失败", e);
            return Result.error("测试翻译失败: " + e.getMessage());
        }
    }

    /**
     * 测试中文分词功能
     */
    @PostMapping("/test/segment")
    public Result<String> testSegment(@RequestParam("text") String text) {
        try {
            log.info("测试分词: {}", text.substring(0, Math.min(50, text.length())));

            String segmented = translationService.segmentText(text);

            StringBuilder result = new StringBuilder();
            result.append("📝 分词测试结果:\\n\\n");
            result.append("原文: ").append(text).append("\\n\\n");
            result.append("分词结果: ").append(segmented).append("\\n\\n");
            result.append("服务状态: ").append(((com.video.service.impl.TranslationServiceImpl) translationService)
                    .isTranslationServiceAvailable() ? "API模式" : "模拟模式");

            return Result.success(result.toString());
        } catch (Exception e) {
            log.error("测试分词失败", e);
            return Result.error("测试分词失败: " + e.getMessage());
        }
    }

    /**
     * 完整流程测试：音频提取 -> 语音识别 -> 翻译
     */
    @PostMapping("/test/full-pipeline/{taskId}")
    public Result<String> testFullPipeline(@PathVariable Long taskId) {
        try {
            log.info("测试完整处理流程: {}", taskId);
            VideoTask task = videoService.getTask(taskId);

            StringBuilder result = new StringBuilder();
            result.append("🔄 完整流程测试:\\n\\n");
            result.append("任务ID: ").append(taskId).append("\\n");
            result.append("视频文件: ").append(task.getFileName()).append("\\n\\n");

            // 步骤1: 音频提取
            result.append("步骤1: 音频提取...\\n");
            try {
                String audioPath = audioExtractionService.extractAudio(task.getFilePath());
                AudioExtractionService.AudioInfo audioInfo = audioExtractionService.getAudioInfo(audioPath);
                result.append("✅ 音频提取成功 (").append(String.format("%.1f", audioInfo.getDuration())).append("秒)\\n\\n");

                // 步骤2: 语音识别
                result.append("步骤2: 语音识别...\\n");
                String transcription = whisperService.transcribe(task.getFilePath());
                result.append("✅ 语音识别成功\\n");
                result.append("识别文本: ").append(transcription.substring(0, Math.min(100, transcription.length())))
                        .append("...\\n\\n");

                // 步骤3: 翻译
                result.append("步骤3: 中文翻译成越南语...\\n");
                String translation = translationService.translate(transcription, "zh", "vi");
                result.append("✅ 翻译成功\\n");
                result.append("翻译结果: ").append(translation.substring(0, Math.min(100, translation.length())))
                        .append("...\\n\\n");

                // 步骤4: 分词
                result.append("步骤4: 中文分词...\\n");
                String segmented = translationService.segmentText(transcription);
                result.append("✅ 分词成功\\n");
                result.append("分词结果: ").append(segmented.substring(0, Math.min(100, segmented.length())))
                        .append("...\\n\\n");

                result.append("🎉 完整流程测试成功！");

            } catch (Exception e) {
                result.append("❌ 流程执行失败: ").append(e.getMessage());
            }

            return Result.success(result.toString());
        } catch (Exception e) {
            log.error("完整流程测试失败", e);
            return Result.error("完整流程测试失败: " + e.getMessage());
        }
    }
}