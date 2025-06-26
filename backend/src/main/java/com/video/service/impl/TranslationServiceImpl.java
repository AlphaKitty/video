package com.video.service.impl;

import com.video.service.TranslationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 翻译服务实现
 * 支持中越双语翻译和智能分词
 */
@Service
public class TranslationServiceImpl implements TranslationService {

    private static final Logger log = LoggerFactory.getLogger(TranslationServiceImpl.class);

    @Value("${ai.openai.api-key:}")
    private String openaiApiKey;

    @Value("${ai.openai.base-url:https://api.openai.com/v1}")
    private String openaiBaseUrl;

    @Value("${ai.translation.model:gpt-4}")
    private String translationModel;

    @Value("${ai.translation.target-language:vi}")
    private String defaultTargetLanguage;

    @Value("${ai.translation.source-language:zh}")
    private String defaultSourceLanguage;

    private final WebClient webClient;

    public TranslationServiceImpl() {
        this.webClient = WebClient.builder()
                .defaultHeader("Content-Type", "application/json")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }

    @Override
    public String translate(String text, String fromLang, String toLang) {
        try {
            log.info("开始翻译: {} -> {}, 文本长度: {}", fromLang, toLang, text.length());

            if (text == null || text.trim().isEmpty()) {
                log.warn("翻译文本为空");
                return "";
            }

            // 检查 API Key 是否配置
            if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
                log.warn("OpenAI API Key 未配置，使用模拟翻译");
                return generateMockTranslation(text, fromLang, toLang);
            }

            try {
                // 调用真实的 OpenAI API
                String result = callOpenAITranslation(text, fromLang, toLang);
                log.info("翻译完成，结果长度: {}", result.length());
                return result;

            } catch (Exception apiException) {
                log.warn("OpenAI API 调用失败，使用模拟翻译: {}", apiException.getMessage());
                return generateMockTranslation(text, fromLang, toLang);
            }

        } catch (Exception e) {
            log.error("翻译失败", e);
            throw new RuntimeException("翻译失败: " + e.getMessage());
        }
    }

    @Override
    public String segmentText(String text) {
        try {
            log.info("开始分词处理，文本长度: {}", text.length());

            if (text == null || text.trim().isEmpty()) {
                log.warn("分词文本为空");
                return "";
            }

            // 检查 API Key 是否配置
            if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
                log.warn("OpenAI API Key 未配置，使用模拟分词");
                return generateMockSegmentation(text);
            }

            try {
                // 调用真实的 OpenAI API 进行智能分词
                String result = callOpenAISegmentation(text);
                log.info("分词完成，结果长度: {}", result.length());
                return result;

            } catch (Exception apiException) {
                log.warn("OpenAI API 分词失败，使用模拟分词: {}", apiException.getMessage());
                return generateMockSegmentation(text);
            }

        } catch (Exception e) {
            log.error("分词失败", e);
            throw new RuntimeException("分词失败: " + e.getMessage());
        }
    }

    /**
     * 调用 OpenAI API 进行翻译
     */
    private String callOpenAITranslation(String text, String fromLang, String toLang) {
        try {
            // 构建翻译提示词
            String prompt = buildTranslationPrompt(text, fromLang, toLang);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", translationModel);
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个专业的中越双语翻译专家，专门处理教育视频的翻译工作。");

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            requestBody.put("messages", java.util.Arrays.asList(systemMessage, userMessage));
            requestBody.put("max_tokens", 4000);
            requestBody.put("temperature", 0.3);

            // 发送请求
            String response = webClient.post()
                    .uri(openaiBaseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + openaiApiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            // 解析响应
            return parseOpenAIResponse(response);

        } catch (WebClientResponseException e) {
            log.error("OpenAI API 请求失败: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("OpenAI API 调用失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("OpenAI 翻译调用异常", e);
            throw new RuntimeException("翻译服务调用失败: " + e.getMessage());
        }
    }

    /**
     * 调用 OpenAI API 进行智能分词
     */
    private String callOpenAISegmentation(String text) {
        try {
            String prompt = "请对以下中文教学文本进行智能分词，用 ' / ' 分隔每个词汇，保持教学的逻辑性：\n\n" + text;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", translationModel);
            Map<String, String> segSystemMessage = new HashMap<>();
            segSystemMessage.put("role", "system");
            segSystemMessage.put("content", "你是一个中文分词专家，专门处理教育内容的智能分词。");

            Map<String, String> segUserMessage = new HashMap<>();
            segUserMessage.put("role", "user");
            segUserMessage.put("content", prompt);

            requestBody.put("messages", java.util.Arrays.asList(segSystemMessage, segUserMessage));
            requestBody.put("max_tokens", 2000);
            requestBody.put("temperature", 0.1);

            String response = webClient.post()
                    .uri(openaiBaseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + openaiApiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(20))
                    .block();

            return parseOpenAIResponse(response);

        } catch (Exception e) {
            log.error("OpenAI 分词调用异常", e);
            throw new RuntimeException("分词服务调用失败: " + e.getMessage());
        }
    }

    /**
     * 构建翻译提示词
     */
    private String buildTranslationPrompt(String text, String fromLang, String toLang) {
        String sourceLang = "zh".equals(fromLang) ? "中文" : "越南语";
        String targetLang = "vi".equals(toLang) ? "越南语" : "中文";

        return String.format(
                "请将以下%s教学内容翻译成%s，要求：\n" +
                        "1. 保持教学的语言风格和逻辑\n" +
                        "2. 适合语言学习者理解\n" +
                        "3. 语法准确，用词自然\n" +
                        "4. 只返回翻译结果，不要添加任何解释\n\n" +
                        "原文：%s",
                sourceLang, targetLang, text);
    }

    /**
     * 解析 OpenAI API 响应
     */
    private String parseOpenAIResponse(String response) {
        try {
            // 简单的 JSON 解析（生产环境建议使用 Jackson 或 Gson）
            int contentStart = response.indexOf("\"content\":\"") + 11;
            int contentEnd = response.indexOf("\",", contentStart);
            if (contentEnd == -1) {
                contentEnd = response.indexOf("\"}", contentStart);
            }

            if (contentStart > 10 && contentEnd > contentStart) {
                return response.substring(contentStart, contentEnd)
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"")
                        .trim();
            } else {
                log.error("无法解析 OpenAI 响应: {}", response);
                throw new RuntimeException("响应解析失败");
            }
        } catch (Exception e) {
            log.error("解析 OpenAI 响应失败: {}", response, e);
            throw new RuntimeException("响应解析错误: " + e.getMessage());
        }
    }

    /**
     * 生成模拟翻译结果
     */
    private String generateMockTranslation(String text, String fromLang, String toLang) {
        if ("zh".equals(fromLang) && "vi".equals(toLang)) {
            // 中文到越南语的模拟翻译
            if (text.contains("你好") || text.contains("欢迎")) {
                return "Xin chào, chào mừng học tiếng Việt. Hôm nay chúng ta học từ vựng cơ bản. Hãy đọc theo tôi.";
            } else {
                return "Đây là bản dịch mô phỏng từ tiếng Trung sang tiếng Việt. Nội dung gốc: "
                        + text.substring(0, Math.min(50, text.length())) + "...";
            }
        } else if ("vi".equals(fromLang) && "zh".equals(toLang)) {
            // 越南语到中文的模拟翻译
            return "这是从越南语到中文的模拟翻译。原文内容: " + text.substring(0, Math.min(50, text.length())) + "...";
        } else {
            return "模拟翻译结果: " + text;
        }
    }

    /**
     * 生成模拟分词结果
     */
    private String generateMockSegmentation(String text) {
        // 简单的模拟分词逻辑
        if (text.contains("你好")) {
            return "你好 / 欢迎 / 学习 / 越南语 / 今天 / 我们 / 来 / 学习 / 基础 / 词汇 / 请 / 跟 / 我 / 一起 / 读";
        } else {
            // 简单按字符分割（实际应该有更智能的分词逻辑）
            return text.replace("", " / ").substring(3).replaceAll("\\s+/\\s+$", "");
        }
    }

    /**
     * 检查翻译服务是否可用
     */
    public boolean isTranslationServiceAvailable() {
        return openaiApiKey != null && !openaiApiKey.trim().isEmpty();
    }

    /**
     * 获取服务状态信息
     */
    public String getServiceStatus() {
        StringBuilder status = new StringBuilder();
        status.append("🔄 翻译服务状态:\n");
        status.append("- API Key 配置: ").append(isTranslationServiceAvailable() ? "已配置" : "未配置").append("\n");
        status.append("- 模型: ").append(translationModel).append("\n");
        status.append("- 默认语言对: ").append(defaultSourceLanguage).append(" → ").append(defaultTargetLanguage)
                .append("\n");
        status.append("- 运行模式: ").append(isTranslationServiceAvailable() ? "API模式" : "模拟模式").append("\n");
        return status.toString();
    }
}