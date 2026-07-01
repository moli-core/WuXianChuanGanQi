-- =============================================
-- AIOT 无线传感网智能体系统 数据库 V2
-- =============================================
CREATE DATABASE IF NOT EXISTS wireless_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE wireless_db;

-- =============================================
-- 1. 环境数据历史表
-- =============================================
DROP TABLE IF EXISTS env_data;
CREATE TABLE env_data (
    id           BIGINT      AUTO_INCREMENT PRIMARY KEY,
    device_code  VARCHAR(50)  COMMENT '设备编码',
    temperature  DECIMAL(5,2) COMMENT '温度 (℃)',
    humidity     DECIMAL(5,2) COMMENT '湿度 (%)',
    smoke_level  DECIMAL(6,2) COMMENT '烟雾浓度 (ppm)',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    INDEX idx_time (create_time),
    INDEX idx_device (device_code)
) COMMENT '环境数据历史';

-- =============================================
-- 2. 用户表 (扩展版)
-- =============================================
DROP TABLE IF EXISTS user;
CREATE TABLE user (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(200) NOT NULL COMMENT 'BCrypt',
    nickname    VARCHAR(50),
    phone       VARCHAR(20),
    email       VARCHAR(100),
    avatar      VARCHAR(300),
    role        VARCHAR(20)  DEFAULT 'user' COMMENT 'admin/user',
    status      TINYINT      DEFAULT 1 COMMENT '0-禁用 1-正常',
    last_login  DATETIME,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '用户表';

INSERT INTO user (username, password, nickname, role) VALUES
    ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 'admin');

-- =============================================
-- 3. AIOT 设备注册表 (云端管理)
-- =============================================
DROP TABLE IF EXISTS aiot_device;
CREATE TABLE aiot_device (
    id                  BIGINT       AUTO_INCREMENT PRIMARY KEY,
    device_code         VARCHAR(50)  NOT NULL UNIQUE COMMENT '设备唯一编码',
    device_name         VARCHAR(100) COMMENT '设备名称',
    device_type         VARCHAR(30)  DEFAULT 'esp32' COMMENT 'esp32/sensor/gateway',
    ip_address          VARCHAR(50)  COMMENT '设备 IP 地址',
    mac_address         VARCHAR(50)  COMMENT 'MAC 地址',
    mqtt_topic          VARCHAR(100) COMMENT 'MQTT 上报主题',
    firmware_version    VARCHAR(20)  COMMENT '固件版本',
    -- 传感器清单 JSON
    sensor_list         JSON         COMMENT '{"led":true,"light_sensor":true,"pir":true,"screen":true,"temp":true}',
    -- 设备上报的实时状态
    led_status          TINYINT      DEFAULT 0 COMMENT 'LED 0-关 1-开',
    mode                VARCHAR(10)  DEFAULT 'manual' COMMENT 'auto/manual',
    light_sensor        INT          COMMENT '光敏值',
    pir_status          TINYINT      DEFAULT 0 COMMENT '人体感应 0-无人 1-有人',
    screen_status       TINYINT      DEFAULT 0 COMMENT '屏幕 0-息屏 1-亮屏',
    wifi_rssi           INT          COMMENT 'WiFi 信号强度',
    tcp_connected       TINYINT      DEFAULT 1 COMMENT 'TCP 连接状态',
    -- 管理字段
    online_status       TINYINT      DEFAULT 0 COMMENT '0-离线 1-在线',
    last_report_time    DATETIME     COMMENT '最后一次数据上报时间',
    register_source     VARCHAR(20)  DEFAULT 'manual' COMMENT 'manual/auto(自动注册)',
    remarks             VARCHAR(500) COMMENT '备注',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_device_code (device_code),
    INDEX idx_online (online_status),
    INDEX idx_type (device_type)
) COMMENT 'AIOT 设备云端注册表';

-- =============================================
-- 4. 设备操作日志表 (扩展版)
-- =============================================
DROP TABLE IF EXISTS operation_log;
CREATE TABLE operation_log (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    device_code  VARCHAR(50)  COMMENT '设备编码',
    device_name  VARCHAR(100) COMMENT '设备名称',
    action       VARCHAR(50)  COMMENT '操作动作 (led:on/fan:off/screen:on/mode:auto...)',
    source       VARCHAR(20)  COMMENT '来源 (voice/manual/auto/model(大模型)/linkage(联动))',
    operator     VARCHAR(50)  COMMENT '操作人',
    remark       VARCHAR(200) COMMENT '备注',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    INDEX idx_device (device_code),
    INDEX idx_time (create_time),
    INDEX idx_source (source)
) COMMENT '设备操作日志';

-- =============================================
-- 5. 语音指令日志表
-- =============================================
DROP TABLE IF EXISTS voice_log;
CREATE TABLE voice_log (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    raw_text        TEXT         COMMENT '语音识别原始文本',
    semantic_result VARCHAR(300) COMMENT '语义解析结果',
    device_command  VARCHAR(200) COMMENT '设备指令',
    is_valid        TINYINT      DEFAULT 1 COMMENT '0-无效 1-有效',
    parse_method    VARCHAR(20)  DEFAULT 'local' COMMENT 'local/model(大模型解析)',
    source          VARCHAR(20)  DEFAULT 'wechat' COMMENT 'hardware/wechat/app',
    operator        VARCHAR(50)  COMMENT '操作人',
    tts_response    TEXT         COMMENT 'TTS 语音回复文本',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    INDEX idx_time (create_time),
    INDEX idx_valid (is_valid),
    INDEX idx_method (parse_method)
) COMMENT '语音指令日志';

-- =============================================
-- 6. 异常告警日志表
-- =============================================
DROP TABLE IF EXISTS alert_log;
CREATE TABLE alert_log (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    device_code  VARCHAR(50),
    alert_type   VARCHAR(30)  NOT NULL COMMENT '告警类型',
    alert_level  TINYINT      DEFAULT 1 COMMENT '1-普通 2-严重 3-紧急',
    content      VARCHAR(500) COMMENT '告警内容',
    env_value    DECIMAL(6,2) COMMENT '触发值',
    is_handled   TINYINT      DEFAULT 0 COMMENT '0-未处理 1-已处理',
    handled_by   VARCHAR(50),
    handled_time DATETIME,
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    INDEX idx_type (alert_type),
    INDEX idx_time (create_time),
    INDEX idx_handled (is_handled)
) COMMENT '异常告警记录';

-- =============================================
-- 7. 服务器实例管理表
-- =============================================
DROP TABLE IF EXISTS server_instance;
CREATE TABLE server_instance (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
    server_name   VARCHAR(100) NOT NULL COMMENT '服务器名称',
    listen_port   INT          NOT NULL COMMENT '监听端口',
    protocol      VARCHAR(10)  DEFAULT 'mqtt' COMMENT 'mqtt/http/tcp',
    server_type   VARCHAR(20)  DEFAULT 'mqtt_broker' COMMENT '服务器类型',
    description   VARCHAR(300) COMMENT '描述',
    status        VARCHAR(20)  DEFAULT 'stopped' COMMENT 'running/stopped/error',
    pid           INT          COMMENT '进程 PID',
    log_path      VARCHAR(300) COMMENT '日志文件路径',
    last_start    DATETIME     COMMENT '最后启动时间',
    last_stop     DATETIME     COMMENT '最后停止时间',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT 'AIOT 后端服务器实例';

-- =============================================
-- 8. 服务器事件日志表
-- =============================================
DROP TABLE IF EXISTS server_event;
CREATE TABLE server_event (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    server_id    BIGINT       COMMENT '关联服务器 ID',
    event_type   VARCHAR(30)  NOT NULL COMMENT 'device_online/device_offline/device_report/device_id_report/abnormal_disconnect/server_start/server_stop/error',
    device_code  VARCHAR(50)  COMMENT '关联设备编码',
    content      VARCHAR(500) COMMENT '事件详情',
    raw_data     TEXT         COMMENT '原始数据',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    INDEX idx_type (event_type),
    INDEX idx_device (device_code),
    INDEX idx_time (create_time),
    INDEX idx_server (server_id)
) COMMENT '服务器事件日志';

-- =============================================
-- 9. AI 模型配置表
-- =============================================
DROP TABLE IF EXISTS ai_model_config;
CREATE TABLE ai_model_config (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
    model_name    VARCHAR(100) NOT NULL COMMENT '模型名称',
    provider      VARCHAR(30)  NOT NULL DEFAULT 'openai' COMMENT 'openai/tongyi/deepseek/qwen',
    api_url       VARCHAR(300) NOT NULL COMMENT 'API 地址',
    api_key       VARCHAR(500) NOT NULL COMMENT 'API Key (加密存储)',
    model_id      VARCHAR(100) COMMENT '模型 ID (如 gpt-4, qwen-turbo)',
    temperature   DECIMAL(3,2) DEFAULT 0.70 COMMENT '温度参数',
    max_tokens    INT          DEFAULT 2048 COMMENT '最大 Token',
    system_prompt TEXT         COMMENT '系统提示词',
    is_default    TINYINT      DEFAULT 0 COMMENT '是否默认模型',
    is_enabled    TINYINT      DEFAULT 1 COMMENT '是否启用',
    sort_order    INT          DEFAULT 0 COMMENT '排序',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_model_name (model_name)
) COMMENT 'AI 大模型配置';

-- 插入默认模型配置
INSERT INTO ai_model_config (model_name, provider, api_url, api_key, model_id, is_default, system_prompt) VALUES
    ('通义千问 Turbo', 'tongyi', 'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions', 'sk-your-key', 'qwen-turbo', 1,
     '你是一个 AIOT 智能家居助手。你可以根据用户自然语言控制设备，可用设备: LED灯、风扇、蜂鸣器、屏幕。响应格式: {"action":"设备:操作","text":"回复文本"}');

-- =============================================
-- 10. AI 对话消息表
-- =============================================
DROP TABLE IF EXISTS chat_message;
CREATE TABLE chat_message (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    session_id   VARCHAR(50)  NOT NULL COMMENT '会话 ID',
    user_id      BIGINT       COMMENT '用户 ID',
    model_id     BIGINT       COMMENT '使用的模型 ID',
    role         VARCHAR(20)  NOT NULL COMMENT 'user/assistant/system',
    content      TEXT         NOT NULL COMMENT '消息内容',
    -- 如果消息包含设备控制动作
    action_result TEXT        COMMENT '设备执行结果 JSON',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    INDEX idx_session (session_id),
    INDEX idx_user (user_id),
    INDEX idx_time (create_time)
) COMMENT 'AI 对话消息历史';

-- =============================================
-- 11. TTS 语音缓存表
-- =============================================
DROP TABLE IF EXISTS tts_cache;
CREATE TABLE tts_cache (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    text_hash    VARCHAR(64)  NOT NULL UNIQUE COMMENT '文本 SHA256 哈希',
    text_content TEXT         NOT NULL COMMENT '原始文本',
    audio_path   VARCHAR(300) COMMENT '音频文件路径',
    audio_url    VARCHAR(300) COMMENT '音频访问 URL',
    voice        VARCHAR(50)  DEFAULT 'default' COMMENT '语音风格',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE KEY uk_text_hash (text_hash)
) COMMENT 'TTS 语音缓存';
