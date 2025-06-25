package com.video.service.impl;

// import com.video.mapper.VideoTaskMapper;
import com.video.model.VideoTask;
import com.video.service.VideoService;
import com.video.service.WhisperService;
import com.video.service.TranslationService;
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
    // 使用内存存储替代数据库
    private final ConcurrentHashMap<Long, VideoTask> taskStorage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    public VideoServiceImpl(WhisperService whisperService, TranslationService translationService) {
        this.whisperService = whisperService;
        this.translationService = translationService;
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
            
            // 创建任务
            VideoTask task = new VideoTask();
            task.setId(idGenerator.getAndIncrement());
            task.setFileName(fileName);
            task.setFilePath(filePath.toString());
            task.setOriginalName(file.getOriginalFilename());
            task.setFileSize(file.getSize());
            task.setStatus("UPLOADED");
            task.setProgress(0);
            task.setCurrentStep("文件上传完成");
            task.setCreateTime(LocalDateTime.now());
            task.setUpdateTime(LocalDateTime.now());
            
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
} 