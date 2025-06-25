package com.video.controller;

import com.video.common.Result;
import com.video.model.VideoTask;
import com.video.service.VideoService;
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
    
    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }
    
    /**
     * 上传视频文件
     */
    @PostMapping("/upload")
    public Result<VideoTask> uploadVideo(@RequestParam("file") MultipartFile file) {
        try {
            log.info("开始上传视频文件: {}", file.getOriginalFilename());
            VideoTask task = videoService.uploadVideo(file);
            return Result.success(task);
        } catch (Exception e) {
            log.error("视频上传失败", e);
            return Result.error("视频上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 开始处理视频
     */
    @PostMapping("/process/{taskId}")
    public Result<Void> processVideo(@PathVariable Long taskId) {
        try {
            log.info("开始处理视频任务: {}", taskId);
            videoService.processVideo(taskId);
            return Result.success();
        } catch (Exception e) {
            log.error("视频处理失败", e);
            return Result.error("视频处理失败: " + e.getMessage());
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
} 