package jiaju.multagent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
/**
 * 全屋智能家居控制REST控制器
 * <p>
 * 提供HTTP接口供前端或其他服务调用，接收用户自然语言指令，
 * 通过ReactAgent智能体处理后返回执行结果。
 * </p>
 *
 * @author jiaju.multagent
 * @version 1.0
 */
@RestController
public class HomeController {
    /**
     * 房间管理器Agent实例
     * 通过构造函数注入，负责处理用户指令并调用相应工具
     */
    private final ReactAgent roomManagerAgent;
    /**
     * 构造函数注入ReactAgent
     *
     * @param roomManagerAgent 房间管理器Agent，由Spring容器自动注入
     */
    public HomeController(ReactAgent roomManagerAgent) {
        this.roomManagerAgent = roomManagerAgent;
    }
    /**
     * POST方式处理用户指令
     * <p>
     * 接收用户自然语言输入，调用Agent进行智能处理，返回执行结果。
     * 支持JSON格式的请求体，直接传入用户输入的文本。
     * </p>
     *
     * @param userInput 用户输入的文本指令，如"我有点热"、"打开客厅灯"等
     * @return Agent处理后的响应文本，或错误信息
     */
    @PostMapping("/multchat")
    public String chat(@RequestBody String userInput) {
        try {
            // 1. 调用Agent的invoke方法处理用户输入
            // invoke方法会触发ReAct循环：推理->行动->观察->再推理，直到完成任务
            Optional<OverAllState> result = roomManagerAgent.invoke(userInput);

            // 2. 判断是否为空
            if (result.isEmpty()) {
                return "系统暂时无法处理，请稍后重试";
            }

            // 3. 获取Agent执行完成后的完整状态对象
            OverAllState state = result.get();

            // 4. 从状态中提取final_answer（最终答案）
            String answer = extractAnswer(state);
            return answer != null ? answer : "抱歉，我暂时无法处理这个请求";

        } catch (Exception e) {
            e.printStackTrace();
            return "处理请求时发生错误: " + e.getMessage();
        }
    }
    /**
     * GET方式处理用户指令
     * <p>
     * 通过URL参数传递用户输入，方便测试和浏览器访问。
     * 内部调用POST方法实现，保持处理逻辑一致。
     * </p>
     *
     * @param q 用户输入的文本指令，通过查询参数传递，如 ?q=我有点热
     * @return Agent处理后的响应文本
     */
    @GetMapping("/multchat")
    public String chatGet(@RequestParam String q) {
        return chat(q);
    }

    /**
     * 从OverAllState中提取final_answer的文本内容
     * <p>
     * OverAllState是Agent执行过程中的状态容器，存储了中间结果和最终输出。
     * 本方法尝试从多个可能的key中提取答案，并处理不同的数据类型。
     * </p>
     *
     * @param state Agent执行完成后的状态对象
     * @return 提取出的答案文本，如果提取失败则返回null
     */
    private String extractAnswer(OverAllState state) {
        try {
            // ✅ 使用 value() 方法获取 final_answer
            // final_answer是Agent配置中指定的输出键名，存储了最终响应
            Optional<Object> answerOpt = state.value("final_answer");

            if (answerOpt.isPresent()) {
                Object output = answerOpt.get();
                // 处理AssistantMessage类型（Spring AI的标准消息类型）
                if (output instanceof AssistantMessage) {
                    return ((AssistantMessage) output).getText();
                }
                // 处理String类型
                if (output instanceof String) {
                    return (String) output;
                }
                // 如果是其他类型，尝试 toString
                return output.toString();
            }

            // 备选方式：尝试从其他 key 获取
            // 某些Agent版本可能使用"output"作为输出键名
            Optional<Object> outputOpt = state.value("output");
            if (outputOpt.isPresent()) {
                Object output = outputOpt.get();
                if (output instanceof AssistantMessage) {
                    return ((AssistantMessage) output).getText();
                }
                if (output instanceof String) {
                    return (String) output;
                }
            }

            // 如果都获取不到，返回 null
            return null;

        } catch (Exception e) {
            // 忽略异常
            return null;
        }
    }
}