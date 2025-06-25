package com.video.service;

/**
 * 翻译服务接口
 */
public interface TranslationService {
    
    /**
     * 翻译文本
     * @param text 原文本
     * @param fromLang 源语言
     * @param toLang 目标语言
     * @return 翻译结果
     */
    String translate(String text, String fromLang, String toLang);
    
    /**
     * 分词处理
     * @param text 原文本
     * @return 分词结果
     */
    String segmentText(String text);
} 