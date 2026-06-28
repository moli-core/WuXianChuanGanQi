package jiaju.multagent;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * 全屋智能家居工具类
 * <p>
 * 本类定义了三个核心家居设备控制工具，通过Spring AI的@Tool注解暴露给AI模型。
 * 这些工具是智能体与物理设备之间的桥梁，将自然语言指令转化为具体的设备操作。
 * </p>
 * <p>
 * <b>设计理念：</b>
 * 每个工具方法都代表一种设备控制能力，AI模型会根据用户的意图自动选择合适的工具调用。
 * 这体现了"工具赋能"的思想，让AI不仅仅能对话，还能实际操作设备。
 * </p>
 *

 * @version 1.0
 */
@Component
public class HomeTools {

    /**
     * 空调控制工具
     * <p>
     * 控制指定房间的空调温度，支持制冷、制热等多种模式。
     * 当用户表达温度不适（热/冷）或明确要求调温时，AI模型会调用此工具。
     * </p>
     * <p>
     * <b>使用场景示例：</b>
     * <ul>
     *   <li>"我有点热" → 自动调用此工具降低温度</li>
     *   <li>"把卧室空调调到26度" → 直接指定温度和位置</li>
     *   <li>"客厅太冷了" → 自动调用此工具升高温度</li>
     * </ul>
     * </p>
     *
     * @param location 房间位置，如"客厅"、"卧室"、"书房"等
     * @param temperature 目标温度值，单位摄氏度（℃），通常在16-30℃之间
     * @return 操作结果描述，告知用户执行状态
     */
    @Tool(description = "控制指定房间的空调温度，用户说'热'/'冷'/'调温度'时调用")
    public String acControl(String location, int temperature) {
        System.out.println("✅ [空调] " + location + " 已设定为 " + temperature + "℃");
        return "已将" + location + "的空调温度设定为" + temperature + "℃";
    }

    // ========== Tool 2: 灯光控制 ==========
    /**
     * 灯光控制工具
     * <p>
     * 控制指定房间的灯光开关状态。
     * 当用户表达光线需求（亮/暗）或明确要求开关灯时，AI模型会调用此工具。
     * </p>
     * <p>
     * <b>使用场景示例：</b>
     * <ul>
     *   <li>"开灯" → 默认打开当前房间灯光</li>
     *   <li>"把卧室灯关掉" → 关闭指定房间灯光</li>
     *   <li>"客厅太暗了" → 自动调用此工具开灯</li>
     *   <li>"睡觉了" → 可能组合调用关闭灯光</li>
     * </ul>
     * </p>
     *
     * @param location 房间位置，如"客厅"、"卧室"、"书房"等
     * @param action 操作类型，值为 "on"（开灯）或 "off"（关灯），不区分大小写
     * @return 操作结果描述，告知用户执行状态
     */
    @Tool(description = "控制指定房间的灯光开关，用户说'开灯'/'关灯'时调用")
    public String lightControl(String location, String action) {
        String status = "on".equalsIgnoreCase(action) ? "开启" : "关闭";
        System.out.println("✅ [灯光] " + location + " 已" + status);
        return "已将" + location + "的灯光" + status;
    }

    // ========== Tool 3: 窗帘控制 ==========
    /**
     * 窗帘控制工具
     * <p>
     * 控制指定房间的窗帘开关状态，调节室内光线和隐私保护。
     * 当用户表达隐私需求或阳光调节时，AI模型会调用此工具。
     * </p>
     * <p>
     * <b>使用场景示例：</b>
     * <ul>
     *   <li>"拉开窗帘" → 打开窗帘让阳光进入</li>
     *   <li>"把卧室窗帘拉上" → 关闭窗帘保护隐私</li>
     *   <li>"太晒了" → 自动调用此工具关闭窗帘</li>
     *   <li>"睡觉了" → 可能组合调用关闭窗帘</li>
     * </ul>
     * </p>
     *
     * @param location 房间位置，如"客厅"、"卧室"、"书房"等
     * @param action 操作类型，值为 "on"（打开窗帘）或 "off"（关闭窗帘），不区分大小写
     * @return 操作结果描述，告知用户执行状态
     */
    @Tool(description = "控制指定房间的窗帘开关，用户说'开窗帘'/'关窗帘'时调用")
    public String curtainControl(String location, String action) {
        String status = "on".equalsIgnoreCase(action) ? "打开" : "关闭";
        System.out.println("✅ [窗帘] " + location + " 已" + status);
        return "已将" + location + "的窗帘" + status;
    }
}