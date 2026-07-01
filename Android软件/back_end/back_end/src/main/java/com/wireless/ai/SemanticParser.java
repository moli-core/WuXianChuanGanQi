package com.wireless.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 语义解析模块
 *
 * 从自然语言文本中提取设备控制意图，映射为标准设备指令。
 *
 * 扩展方向: 接入大模型进行更智能的意图理解
 */
@Slf4j
@Component
public class SemanticParser {

    /**
     * 设备指令映射表
     * 正则 → "deviceCode:action"
     */
    private static final Map<String, String> COMMAND_PATTERNS = new HashMap<>();

    static {
        // 灯光
        COMMAND_PATTERNS.put("(打开|开启|开).*灯|灯.*(开|打开|亮)", "led:1");
        COMMAND_PATTERNS.put("(关闭|关).*灯|灯.*(关|关闭|灭)", "led:0");
        // 风扇
        COMMAND_PATTERNS.put("(打开|开启|开).*(风扇|通风)|(风扇|通风).*(开|打开)", "fan:1");
        COMMAND_PATTERNS.put("(关闭|关).*(风扇|通风)|(风扇|通风).*(关|关闭)", "fan:0");
        // 蜂鸣器
        COMMAND_PATTERNS.put("(打开|开启|开).*(蜂鸣|警报|报警)|(蜂鸣|警报|报警).*(开|打开|响)", "buzzer:1");
        COMMAND_PATTERNS.put("(关闭|关).*(蜂鸣|警报|报警)|(蜂鸣|警报|报警).*(关|关闭|停)", "buzzer:0");
        // 全部
        COMMAND_PATTERNS.put("(全部|所有|一键).*(打开|开启|开)", "all:1");
        COMMAND_PATTERNS.put("(全部|所有|一键).*(关闭|关)", "all:0");
    }

    /**
     * 解析自然语言文本，提取设备控制指令
     *
     * @param text 语音识别文本
     * @return 设备指令 "deviceCode:action" 或 null (非设备控制)
     */
    public String parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        log.debug("语义解析: {}", text);

        for (Map.Entry<String, String> entry : COMMAND_PATTERNS.entrySet()) {
            Pattern pattern = Pattern.compile(entry.getKey());
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                log.info("语义匹配: \"{}\" → {}", text, entry.getValue());
                return entry.getValue();
            }
        }

        log.debug("非设备控制类语音，已过滤: {}", text);
        return null;
    }
}
