package jiaju.example;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ChatController {

    private final ChatClient chatClient;

    @Autowired
    public ChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String query) {

        // 调用 call 方法发送问题并获取回答
        return chatClient.prompt(query).call().content();
    }

    /**
     * 接收前端消息，返回AI回复
     */
    @PostMapping("/chat1")
    public Map<String, Object> chat1(@RequestBody ChatRequest request) {
        String message = request.getMessage();
        System.out.println("收到消息：" + message);

        // 调用AI
        String response = chatClient.prompt(message).call().content();
        System.out.println("AI回复：" + response);

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("content", response);
        result.put("success", true);
        return result;
    }
}
