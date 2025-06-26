package com.video.service;

/**
 * 音频提取服务接口
 */
public interface AudioExtractionService {

    /**
     * 从视频文件提取音频
     * 
     * @param videoPath 视频文件路径
     * @return 音频文件路径
     */
    String extractAudio(String videoPath);

    /**
     * 获取音频文件信息
     * 
     * @param audioPath 音频文件路径
     * @return 音频信息（时长、采样率等）
     */
    AudioInfo getAudioInfo(String audioPath);

    /**
     * 音频信息类
     */
    class AudioInfo {
        private double duration;
        private int sampleRate;
        private int channels;

        public AudioInfo(double duration, int sampleRate, int channels) {
            this.duration = duration;
            this.sampleRate = sampleRate;
            this.channels = channels;
        }

        // Getters
        public double getDuration() {
            return duration;
        }

        public int getSampleRate() {
            return sampleRate;
        }

        public int getChannels() {
            return channels;
        }

        // Setters
        public void setDuration(double duration) {
            this.duration = duration;
        }

        public void setSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
        }

        public void setChannels(int channels) {
            this.channels = channels;
        }
    }
}