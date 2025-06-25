package com.video.service;

/**
 * Whisper 语音识别服务
 */
public interface WhisperService {
    
    /**
     * 转录音频文件为文本
     * @param audioFilePath 音频文件路径
     * @return 识别的文本内容
     */
    String transcribe(String audioFilePath);
} 