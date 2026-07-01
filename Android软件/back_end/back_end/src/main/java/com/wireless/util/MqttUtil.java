package com.wireless.util;

/**
 * MQTT 工具类
 */
public class MqttUtil {

    private MqttUtil() {
    }

    /**
     * 构造设备控制指令 JSON
     * @param deviceCode 设备编码
     * @param action 0-关 1-开
     * @return JSON 字符串
     */
    public static String buildCommand(String deviceCode, int action) {
        return String.format("{\"cmd\":\"%s:%s\"}", deviceCode, action == 1 ? "on" : "off");
    }

    /**
     * 解析设备上报的 JSON
     * @param payload JSON 字符串
     * @param key 字段名
     * @return 字段值
     */
    public static String extractValue(String payload, String key) {
        try {
            return JsonUtil.parseObject(payload).getString(key);
        } catch (Exception e) {
            return null;
        }
    }
}
