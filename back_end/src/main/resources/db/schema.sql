-- =============================================
-- 无线智能家居系统 — 数据库建表脚本
-- 创建数据库
-- =============================================
CREATE DATABASE IF NOT EXISTS wireless_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE wireless_db;

-- =============================================
-- 1. 环境数据历史表 (每秒采集)
-- =============================================
DROP TABLE IF EXISTS env_data;
CREATE TABLE env_data (
    id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    temperature DECIMAL(5,2)    COMMENT '温度 (℃)',
    humidity    DECIMAL(5,2)    COMMENT '湿度 (%)',
    smoke_level DECIMAL(6,2)    COMMENT '烟雾浓度 (ppm)',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '采集时间',
    INDEX idx_create_time (create_time),
    INDEX idx_temp (temperature),
    INDEX idx_smoke (smoke_level)
) COMMENT '环境数据历史记录';

-- =============================================
-- 2. 设备当前状态表
-- =============================================
DROP TABLE IF EXISTS device_status;
CREATE TABLE device_status (
    id          INT             AUTO_INCREMENT PRIMARY KEY,
    device_code VARCHAR(50)     NOT NULL UNIQUE COMMENT '设备编码 (led/fan/buzzer)',
    device_name VARCHAR(50)     NOT NULL COMMENT '设备名称',
    status      TINYINT         DEFAULT 0 COMMENT '0-关闭 1-开启',
    update_time DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间'
) COMMENT '设备当前状态';

-- 初始数据
INSERT INTO device_status (device_code, device_name, status) VALUES
    ('led',    '灯光',   0),
    ('fan',    '风扇',   0),
    ('buzzer', '蜂鸣器', 0);

-- =============================================
-- 3. 设备操作日志表
-- =============================================
DROP TABLE IF EXISTS operation_log;
CREATE TABLE operation_log (
    id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    device_code VARCHAR(50)     COMMENT '设备编码',
    device_name VARCHAR(50)     COMMENT '设备名称',
    action      TINYINT         COMMENT '操作动作 0-关闭 1-开启',
    source      VARCHAR(20)     COMMENT '控制来源 voice/manual/auto',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '操作时间',
    INDEX idx_device_code (device_code),
    INDEX idx_create_time (create_time),
    INDEX idx_source (source)
) COMMENT '设备启停操作记录';

-- =============================================
-- 4. 语音指令日志表
-- =============================================
DROP TABLE IF EXISTS voice_log;
CREATE TABLE voice_log (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    raw_text        TEXT            COMMENT '语音识别原始文本',
    semantic_result VARCHAR(200)    COMMENT '语义解析结果 (提取的意图)',
    device_command  VARCHAR(100)    COMMENT '匹配的设备指令 (如 led:on)',
    is_valid        TINYINT         DEFAULT 1 COMMENT '是否有效设备控制指令 0-否 1-是',
    source          VARCHAR(20)     DEFAULT 'hardware' COMMENT '语音来源 hardware/wechat',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '识别时间',
    INDEX idx_create_time (create_time),
    INDEX idx_is_valid (is_valid)
) COMMENT '语音指令日志';

-- =============================================
-- 5. 异常告警日志表
-- =============================================
DROP TABLE IF EXISTS alert_log;
CREATE TABLE alert_log (
    id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    alert_type  VARCHAR(30)     NOT NULL COMMENT '告警类型 temp_high/smoke_high/humidity_abnormal',
    alert_level TINYINT         DEFAULT 1 COMMENT '告警等级 1-普通 2-严重 3-紧急',
    content     VARCHAR(500)    COMMENT '告警内容描述',
    env_value   DECIMAL(6,2)    COMMENT '触发告警的环境值',
    is_handled  TINYINT         DEFAULT 0 COMMENT '是否已处理 0-未处理 1-已处理',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '告警时间',
    INDEX idx_alert_type (alert_type),
    INDEX idx_create_time (create_time),
    INDEX idx_is_handled (is_handled)
) COMMENT '异常告警记录';

-- =============================================
-- 6. 用户表 (拓展)
-- =============================================
DROP TABLE IF EXISTS user;
CREATE TABLE user (
    id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)     NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(200)    NOT NULL COMMENT '密码 (BCrypt)',
    nickname    VARCHAR(50)     COMMENT '昵称',
    role        VARCHAR(20)     DEFAULT 'user' COMMENT '角色 admin/user',
    status      TINYINT         DEFAULT 1 COMMENT '0-禁用 1-正常',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '注册时间'
) COMMENT '用户表';

-- 默认管理员
INSERT INTO user (username, password, nickname, role) VALUES
    ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 'admin');
