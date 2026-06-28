package jiaju.multagent;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 全屋智能管家Agent配置类
 * <p>
 * 本配置类负责创建和管理ReactAgent实例，该Agent作为全屋智能的核心控制器，
 * 能够理解用户自然语言指令，并通过调用相应工具完成家居设备的控制。
 * </p>
 *
 * @version 1.0
 */
@Configuration
public class HomeAgentConfig {
    /**
     * DashScope聊天模型实例（通过构造函数注入）
     * 使用阿里云通义千问大模型作为Agent的推理引擎
     */
    private final DashScopeChatModel chatModel;
    /**
     * 构造函数注入DashScopeChatModel
     *
     * @param chatModel DashScope聊天模型，由Spring容器自动注入
     */
    public HomeAgentConfig(DashScopeChatModel chatModel) {
        this.chatModel = chatModel;
    }
    /**
     * 创建房间管理器Agent Bean
     * <p>
     * ReactAgent采用ReAct（Reasoning + Acting）模式，能够：
     * 1. 理解用户的自然语言指令（推理）
     * 2. 调用合适的工具执行操作（行动）
     * 3. 根据执行结果调整后续操作（迭代）
     * </p>
     *
     * @param chatModel 聊天模型，用于理解和生成对话
     * @param homeTools 家居工具类，包含所有可调用的设备控制方法（带有@Tool注解）
     * @return 配置完成的ReactAgent实例
     */
    @Bean
    public ReactAgent roomManagerAgent(ChatModel chatModel, HomeTools homeTools) {
        // 将带有 @Tool 注解的工具类转换为 ToolCallback 数组
        ToolCallback[] tools = ToolCallbacks.from(homeTools);

        return ReactAgent.builder()
                .name("room_manager")           // Agent 名称
                .model(chatModel)               // 设置 ChatModel
                .outputKey("final_answer")
                .instruction("""
                        你是一个全屋智能管家。你需要理解用户对家居设备控制的需求，然后调用合适的工具来执行。
                        用户可能会说"我有点热"、"睡觉了"、"客厅太暗了"等口语化指令。
                        请根据以下规则处理：
                        1. 如果涉及温度调节（热/冷/温度），调用空调工具
                        2. 如果涉及光线（亮/暗/开灯/关灯），调用灯光工具
                        3. 如果涉及隐私/阳光（开窗/关窗/拉窗帘），调用窗帘工具
                        4. 如果用户说"睡觉"等场景指令，可能需要调用多个工具组合
                        5. 如果无法判断，请向用户询问具体需求
                        """)
                .tools(tools)                   // 注册工具
                .build();                       // ✅ 必须调用 build() 结束
    }
}