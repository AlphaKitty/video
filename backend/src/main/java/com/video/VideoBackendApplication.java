package com.video;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 视频转换工具后端服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.video")
@EnableAsync
public class VideoBackendApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(VideoBackendApplication.class, args);
    }
}
