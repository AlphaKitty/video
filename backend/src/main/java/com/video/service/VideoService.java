package com.video.service;

import com.video.model.VideoTask;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 视频处理服务接口
 */
public interface VideoService {
    
    /**
     * 上传视频文件
     */
    VideoTask uploadVideo(MultipartFile file);
    
    /**
     * 处理视频（异步）
     */
    void processVideo(Long taskId);
    
    /**
     * 获取任务状态
     */
    VideoTask getTask(Long taskId);
    
    /**
     * 获取所有任务
     */
    List<VideoTask> getAllTasks();
    
    /**
     * 更新字幕内容
     */
    void updateSubtitle(Long taskId, String subtitleContent);
} 