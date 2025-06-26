package com.video.service.impl;

// import com.video.mapper.VideoTaskMapper;
import com.video.model.VideoTask;
import com.video.service.AudioExtractionService;
import com.video.service.VideoService;
import com.video.service.WhisperService;
import com.video.service.TranslationService;
import com.video.service.impl.AudioExtractionServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 视频处理服务实现
 */
@Service
public class VideoServiceImpl implements VideoService {

    private static final Logger log = LoggerFactory.getLogger(VideoServiceImpl.class);

    private final WhisperService whisperService;
    private final TranslationService translationService;
    private final AudioExtractionService audioExtractionService;
    // 使用内存存储替代数据库
    private final ConcurrentHashMap<Long, VideoTask> taskStorage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public VideoServiceImpl(WhisperService whisperService, TranslationService translationService,
            AudioExtractionService audioExtractionService) {
        this.whisperService = whisperService;
        this.translationService = translationService;
        this.audioExtractionService = audioExtractionService;
    }

    @Value("${file.upload-path:./uploads/}")
    private String uploadPath;

    @Value("${file.temp-path:./temp/}")
    private String tempPath;

    @Override
    public VideoTask uploadVideo(MultipartFile file) {
        try {
            log.info("开始上传文件: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());
            log.info("配置的上传路径: {}", uploadPath);

            // 创建上传目录
            Path uploadDir = Paths.get(uploadPath);
            log.info("解析后的上传目录路径: {}", uploadDir.toAbsolutePath());

            if (!Files.exists(uploadDir)) {
                log.info("上传目录不存在，正在创建: {}", uploadDir.toAbsolutePath());
                Files.createDirectories(uploadDir);
                log.info("上传目录创建成功");
            } else {
                log.info("上传目录已存在: {}", uploadDir.toAbsolutePath());
            }

            // 验证目录是否可写
            if (!Files.isWritable(uploadDir)) {
                throw new IOException("上传目录不可写: " + uploadDir.toAbsolutePath());
            }

            // 生成文件名
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadDir.resolve(fileName);
            log.info("目标文件路径: {}", filePath.toAbsolutePath());

            // 保存文件
            file.transferTo(filePath.toFile());
            log.info("文件保存成功: {}", filePath.toAbsolutePath());

            // 验证文件是否保存成功
            if (!Files.exists(filePath)) {
                throw new IOException("文件保存失败，文件不存在: " + filePath.toAbsolutePath());
            }

            // 立即验证视频文件的完整性和格式
            log.info("开始验证上传的视频文件: {}", filePath.toAbsolutePath());
            String validationError = validateVideoFile(filePath.toString());

            // 创建任务
            VideoTask task = new VideoTask();
            task.setId(idGenerator.getAndIncrement());
            task.setFileName(fileName);
            task.setFilePath(filePath.toString());
            task.setOriginalName(file.getOriginalFilename());
            task.setFileSize(file.getSize());
            task.setCreateTime(LocalDateTime.now());
            task.setUpdateTime(LocalDateTime.now());

            if (validationError != null) {
                // 验证失败
                task.setStatus("UPLOAD_FAILED");
                task.setProgress(0);
                task.setCurrentStep("视频文件验证失败");
                task.setErrorMessage(validationError);
                log.error("视频文件验证失败: {} - {}", filePath, validationError);

                // 删除无效的文件
                try {
                    Files.deleteIfExists(filePath);
                    log.info("已删除无效的视频文件: {}", filePath);
                } catch (IOException e) {
                    log.warn("删除无效文件失败: {}", e.getMessage());
                }

                // 保存失败的任务记录
                taskStorage.put(task.getId(), task);
                throw new RuntimeException("视频文件验证失败: " + validationError);
            } else {
                // 验证成功
                task.setStatus("UPLOADED");
                task.setProgress(0);
                task.setCurrentStep("文件上传并验证完成");
                log.info("视频文件上传并验证成功: {}", filePath);
            }

            // 保存到内存
            taskStorage.put(task.getId(), task);

            log.info("视频文件上传成功: {}, 任务ID: {}", fileName, task.getId());
            return task;

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    @Async
    public void processVideo(Long taskId) {
        VideoTask task = taskStorage.get(taskId);
        if (task == null) {
            log.error("任务不存在: {}", taskId);
            return;
        }

        try {
            // 更新状态为处理中
            updateTaskStatus(task, "PROCESSING", 10, "开始处理视频");

            // 再次验证视频文件（防止文件在上传后被损坏）
            String validationError = validateVideoFile(task.getFilePath());
            if (validationError != null) {
                throw new RuntimeException("视频文件验证失败: " + validationError);
            }

            // 步骤1: 语音识别
            updateTaskStatus(task, "PROCESSING", 20, "语音识别中");
            String chineseText = whisperService.transcribe(task.getFilePath());

            // 步骤2: 翻译处理
            updateTaskStatus(task, "PROCESSING", 50, "翻译处理中");
            String vietnameseText = translationService.translate(chineseText, "zh", "vi");

            // 步骤3: 词组拆分
            updateTaskStatus(task, "PROCESSING", 70, "词组拆分中");
            String segmentedText = translationService.segmentText(chineseText);

            // 步骤4: 生成字幕
            updateTaskStatus(task, "PROCESSING", 90, "生成字幕中");
            String subtitleContent = generateSubtitle(chineseText, vietnameseText, segmentedText);

            // 保存字幕文件
            String subtitlePath = saveSubtitle(taskId, subtitleContent);
            task.setSubtitlePath(subtitlePath);

            // 完成处理
            updateTaskStatus(task, "COMPLETED", 100, "处理完成");

        } catch (Exception e) {
            log.error("视频处理失败", e);
            updateTaskStatus(task, "FAILED", task.getProgress(), "处理失败: " + e.getMessage());
            task.setErrorMessage(e.getMessage());
        }
    }

    @Override
    public VideoTask getTask(Long taskId) {
        VideoTask task = taskStorage.get(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        return task;
    }

    @Override
    public List<VideoTask> getAllTasks() {
        return taskStorage.values().stream()
                .sorted((t1, t2) -> t2.getCreateTime().compareTo(t1.getCreateTime()))
                .collect(Collectors.toList());
    }

    @Override
    public void updateSubtitle(Long taskId, String subtitleContent) {
        VideoTask task = taskStorage.get(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        try {
            String subtitlePath = saveSubtitle(taskId, subtitleContent);
            task.setSubtitlePath(subtitlePath);
            task.setUpdateTime(LocalDateTime.now());

            log.info("字幕更新成功: {}", taskId);
        } catch (Exception e) {
            log.error("字幕更新失败", e);
            throw new RuntimeException("字幕更新失败: " + e.getMessage());
        }
    }

    private void updateTaskStatus(VideoTask task, String status, Integer progress, String currentStep) {
        task.setStatus(status);
        task.setProgress(progress);
        task.setCurrentStep(currentStep);
        task.setUpdateTime(LocalDateTime.now());
        taskStorage.put(task.getId(), task);
        log.info("任务状态更新: {} - {} - {}", task.getId(), progress, currentStep);
    }

    private String generateSubtitle(String chineseText, String vietnameseText, String segmentedText) {
        // 简单的字幕生成逻辑（实际应该根据时间轴生成）
        StringBuilder subtitle = new StringBuilder();
        subtitle.append("1\n");
        subtitle.append("00:00:00,000 --> 00:00:05,000\n");
        subtitle.append(chineseText.substring(0, Math.min(50, chineseText.length()))).append("\n");
        subtitle.append(vietnameseText.substring(0, Math.min(50, vietnameseText.length()))).append("\n\n");

        return subtitle.toString();
    }

    private String saveSubtitle(Long taskId, String content) throws IOException {
        Path tempDir = Paths.get(tempPath);
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        String subtitleFileName = taskId + "_subtitle.srt";
        Path subtitlePath = tempDir.resolve(subtitleFileName);
        Files.write(subtitlePath, content.getBytes("UTF-8"));

        return subtitlePath.toString();
    }

    /**
     * 验证视频文件的完整性和格式
     * 
     * @param videoPath 视频文件路径
     * @return 错误信息，如果验证通过则返回null
     */
    private String validateVideoFile(String videoPath) {
        try {
            log.info("验证视频文件: {}", videoPath);

            // 检查文件是否存在
            Path file = Paths.get(videoPath);
            if (!Files.exists(file)) {
                return "视频文件不存在";
            }

            // 检查文件大小
            long fileSize = Files.size(file);
            if (fileSize == 0) {
                return "视频文件为空";
            }

            if (fileSize < 1024) { // 小于1KB
                return "视频文件过小，可能已损坏";
            }

            log.info("文件基本检查通过: {} (大小: {}字节)", videoPath, fileSize);

            // 检查FFmpeg是否可用
            if (!((AudioExtractionServiceImpl) audioExtractionService).isFFmpegAvailable()) {
                return "FFmpeg不可用，无法验证视频文件格式";
            }

            // 使用AudioExtractionService验证视频文件
            try {
                // 尝试获取音频信息来验证文件格式
                AudioExtractionService.AudioInfo audioInfo = audioExtractionService.getAudioInfo(videoPath);
                if (audioInfo.getDuration() <= 0) {
                    return "视频文件时长异常，可能已损坏";
                }

                log.info("视频文件验证通过: {} (时长: {}秒)", videoPath, audioInfo.getDuration());
                return null; // 验证通过

            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg.contains("moov atom not found") || errorMsg.contains("MP4文件缺少moov原子")) {
                    return "MP4文件损坏：缺少moov原子（文件元数据），可能是上传不完整或文件本身损坏";
                } else if (errorMsg.contains("Invalid data found") || errorMsg.contains("无效数据")) {
                    return "视频文件数据损坏：文件内容无效，请重新上传";
                } else if (errorMsg.contains("No such file") || errorMsg.contains("不存在")) {
                    return "视频文件路径无效或文件已被删除";
                } else if (errorMsg.contains("格式无法识别") || errorMsg.contains("unknown format")) {
                    return "视频文件格式不支持：请使用常见的视频格式（MP4、AVI、MOV等）";
                } else if (errorMsg.contains("被截断") || errorMsg.contains("truncated")) {
                    return "视频文件上传不完整：请重新上传完整的文件";
                } else if (errorMsg.contains("编码格式") || errorMsg.contains("codec")) {
                    return "视频编码格式不支持：请转换为常见的编码格式";
                } else if (errorMsg.contains("头部信息") || errorMsg.contains("header")) {
                    return "视频文件头部损坏：文件结构不完整，请重新生成视频文件";
                } else {
                    return "视频文件格式验证失败：" + errorMsg;
                }
            }

        } catch (Exception e) {
            log.error("视频文件验证过程出错: {} - {}", videoPath, e.getMessage());
            return "视频文件验证过程出错：" + e.getMessage();
        }
    }
}