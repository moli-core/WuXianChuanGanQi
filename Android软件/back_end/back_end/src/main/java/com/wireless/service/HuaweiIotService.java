package com.wireless.service;

import java.util.Map;

/**
 * 华为 IoTDA 云平台服务 (与硬件同学统一)
 */
public interface HuaweiIotService {

    /** 向设备下发属性 */
    void sendProperties(Map<String, Object> command) throws Exception;

    /** 控制 LED 开关 */
    void sendLed(boolean enabled) throws Exception;

    /** 构造 LED 控制指令 */
    Map<String, Object> ledCommand(boolean enabled);

    /** 根据语音文本解析指令并下发 */
    Map<String, Object> sendVoiceCommand(String voiceText) throws Exception;
}
