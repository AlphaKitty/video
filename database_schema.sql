-- 视频转换工具数据库表结构
-- 创建数据库
CREATE DATABASE IF NOT EXISTS video_converter CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE video_converter;

-- 视频任务表
CREATE TABLE video_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADED' COMMENT '状态: UPLOADED, PROCESSING, COMPLETED, FAILED',
    progress INT DEFAULT 0 COMMENT '进度百分比',
    current_step VARCHAR(100) COMMENT '当前步骤描述',
    subtitle_path VARCHAR(500) COMMENT '字幕文件路径',
    output_path VARCHAR(500) COMMENT '输出文件路径',
    error_message TEXT COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频处理任务表';

-- 字幕内容表
CREATE TABLE subtitle_contents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL COMMENT '任务ID',
    sequence_number INT NOT NULL COMMENT '字幕序号',
    start_time VARCHAR(20) NOT NULL COMMENT '开始时间',
    end_time VARCHAR(20) NOT NULL COMMENT '结束时间',
    chinese_text TEXT COMMENT '中文文本',
    vietnamese_text TEXT COMMENT '越南语文本',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES video_tasks(id) ON DELETE CASCADE,
    INDEX idx_task_sequence (task_id, sequence_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字幕内容表';

-- 处理日志表
CREATE TABLE process_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL COMMENT '任务ID',
    step_name VARCHAR(50) NOT NULL COMMENT '步骤名称',
    status VARCHAR(20) NOT NULL COMMENT '状态: STARTED, COMPLETED, FAILED',
    message TEXT COMMENT '日志信息',
    duration_ms BIGINT COMMENT '耗时毫秒',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES video_tasks(id) ON DELETE CASCADE,
    INDEX idx_task_step (task_id, step_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处理日志表'; 