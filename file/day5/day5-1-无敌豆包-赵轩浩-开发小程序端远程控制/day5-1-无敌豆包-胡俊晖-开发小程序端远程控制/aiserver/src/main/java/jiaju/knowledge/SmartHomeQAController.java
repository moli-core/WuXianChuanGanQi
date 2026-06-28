package jiaju.knowledge;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/smarthome/qa")
public class SmartHomeQAController {

    private final SmartHomeQAService qaService;

    public SmartHomeQAController(SmartHomeQAService qaService) {
        this.qaService = qaService;
    }

    /**
     * 1. 智能问答接口（推荐使用）
     * 自动匹配知识库 + LLM 推理
     * 设备+关键字
     */
    @PostMapping("/ask")
    public SmartHomeResponse ask(@RequestBody SmartHomeRequest request) {
        return qaService.ask(request);
    }

    /**
     * 2. 精确匹配接口（先查知识库，找不到才用LLM）
     */
    @PostMapping("/ask/exact")
    public SmartHomeResponse askWithExactMatch(@RequestBody SmartHomeRequest request) {
        return qaService.askWithExactMatch(request);
    }

    /**
     * 3. 获取所有设备类型
     */
    @GetMapping("/device-types")
    public List<String> getDeviceTypes() {
        return qaService.getDeviceTypes();
    }

    /**
     * 4. 获取所有问答对（调试用）
     */
    @GetMapping("/all")
    public Map<String, Object> getAllQAPairs() {
        Map<String, Object> response = new HashMap<>();
        response.put("total", qaService.getAllQAPairs().size());
        response.put("qaPairs", qaService.getAllQAPairs());
        return response;
    }

    /**
     * 5. 健康检查
     */
    @GetMapping("/health")
    public String health() {
        return "SmartHome QA Service is running. Total QA pairs: " + 
               qaService.getAllQAPairs().size();
    }
}