package jiaju.knowledge;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SmartHomeQAService {

    private final ChatClient chatClient;
    private final QAKnowledgeBase qaKnowledgeBase;

    public SmartHomeQAService(ChatClient.Builder chatClientBuilder, 
                              QAKnowledgeBase qaKnowledgeBase) {
        this.qaKnowledgeBase = qaKnowledgeBase;
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 智能问答 - 完整流程
     */
    public SmartHomeResponse ask(SmartHomeRequest request) {
        String question = request.getQuestion();
        String deviceType = request.getDeviceType();
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 搜索知识库
            List<QAPair> matchedQAs = qaKnowledgeBase.search(question, deviceType);
            
            // 2. 获取最佳匹配
            QAPair bestMatch = matchedQAs.isEmpty() ? null : matchedQAs.get(0);
            
            // 3. 构建提示词
            String prompt = buildPrompt(question, deviceType, matchedQAs, bestMatch);
            
            // 4. 调用LLM
            String answer = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            
            // 5. 构建响应
            long processingTime = System.currentTimeMillis() - startTime;
            
            return SmartHomeResponse.builder()
                    .question(question)
                    .answer(answer)
                    .deviceType(deviceType)
                    .processingTimeMs(processingTime)
                    .timestamp(LocalDateTime.now().toString())
                    .matchedCount(matchedQAs.size())
                    .bestMatch(bestMatch != null ? bestMatch.getQuestion() : null)
                    .sources(matchedQAs.stream()
                            .limit(3)
                            .map(QAPair::getQuestion)
                            .collect(Collectors.toList()))
                    .build();
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            return SmartHomeResponse.builder()
                    .question(question)
                    .answer("抱歉，处理您的问题时出现错误：" + e.getMessage())
                    .deviceType(deviceType)
                    .processingTimeMs(processingTime)
                    .timestamp(LocalDateTime.now().toString())
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * 精确匹配模式（先查知识库，匹配不到才用LLM）
     */
    public SmartHomeResponse askWithExactMatch(SmartHomeRequest request) {
        String question = request.getQuestion();
        String deviceType = request.getDeviceType();

        long startTime = System.currentTimeMillis();

        // 1. 先在知识库中精确匹配
        QAPair exactMatch = qaKnowledgeBase.findExactMatch(question);
        if (exactMatch != null) {
            long processingTime = System.currentTimeMillis() - startTime;
            return SmartHomeResponse.builder()
                    .question(question)
                    .answer("【知识库精确匹配】\n" + exactMatch.getAnswer())
                    .deviceType(deviceType)
                    .processingTimeMs(processingTime)
                    .timestamp(LocalDateTime.now().toString())
                    .matchedCount(1)
                    .bestMatch(exactMatch.getQuestion())
                    .isExactMatch(true)
                    .build();
        }

        // 2. 🔥 修改：模糊匹配（如果没有匹配，使用所有知识库）
        List<QAPair> fuzzyMatches = qaKnowledgeBase.findFuzzyMatch(question);

        // 🔥 如果模糊匹配为空，使用所有问答对作为上下文
        if (fuzzyMatches.isEmpty()) {
            // 获取该设备类型的所有问答对，或者全部
            if (deviceType != null && !deviceType.isEmpty()) {
                fuzzyMatches = qaKnowledgeBase.findByDeviceType(deviceType);
            } else {
                fuzzyMatches = qaKnowledgeBase.getAllQAPairs();
            }
            // 限制数量，避免提示词过长
            if (fuzzyMatches.size() > 10) {
                fuzzyMatches = fuzzyMatches.subList(0, 10);
            }
        }

        // 3. 🔥 如果有任何知识库内容，使用 LLM 生成回答
        if (!fuzzyMatches.isEmpty()) {
            String context = formatQAToContext(fuzzyMatches);
            String prompt = String.format("""
                你是一位专业的智能家居技术顾问。
                
                用户问题：%s
                
                以下是智能家居知识库中的相关内容：
                %s
                
                请根据以上知识库内容回答用户问题。
                如果知识库中没有直接相关的信息，请结合最接近的内容给出建议。
                如果完全无法回答，请礼貌地告知用户。
                """, question, context);

            String answer = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            long processingTime = System.currentTimeMillis() - startTime;
            return SmartHomeResponse.builder()
                    .question(question)
                    .answer(answer)
                    .deviceType(deviceType)
                    .processingTimeMs(processingTime)
                    .timestamp(LocalDateTime.now().toString())
                    .matchedCount(fuzzyMatches.size())
                    .bestMatch(fuzzyMatches.get(0).getQuestion())
                    .isExactMatch(false)
                    .build();
        }

        // 4. 知识库完全为空（理论上不会发生）
        long processingTime = System.currentTimeMillis() - startTime;
        return SmartHomeResponse.builder()
                .question(question)
                .answer("抱歉，当前知识库为空。请联系管理员添加知识库内容。")
                .deviceType(deviceType)
                .processingTimeMs(processingTime)
                .timestamp(LocalDateTime.now().toString())
                .matchedCount(0)
                .isExactMatch(false)
                .build();
    }

    /**
     * 获取设备类型列表
     */
    public List<String> getDeviceTypes() {
        return qaKnowledgeBase.getAllDeviceTypes();
    }

    /**
     * 获取所有问答对（调试用）
     */
    public List<QAPair> getAllQAPairs() {
        return qaKnowledgeBase.getAllQAPairs();
    }

    // ========== 私有辅助方法 ==========

    private String buildPrompt(String question, String deviceType, 
                               List<QAPair> matchedQAs, QAPair bestMatch) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一位专业的智能家居技术顾问。\n\n");
        
        // 1. 添加设备类型信息
        if (deviceType != null && !deviceType.isEmpty()) {
            prompt.append("【设备类型】").append(deviceType).append("\n\n");
        }
        
        // 2. 添加最佳匹配（如果有）
        if (bestMatch != null) {
            prompt.append("【最相关问答 - 请优先参考】\n");
            prompt.append("问题：").append(bestMatch.getQuestion()).append("\n");
            prompt.append("答案：").append(bestMatch.getAnswer()).append("\n\n");
        }
        
        // 3. 添加其他相关问答对（最多5条）
        if (matchedQAs.size() > 1) {
            prompt.append("【其他相关问答对】\n");
            matchedQAs.stream()
                    .skip(1)  // 跳过最佳匹配
                    .limit(4)
                    .forEach(qa -> {
                        prompt.append("问题：").append(qa.getQuestion()).append("\n");
                        prompt.append("答案：").append(qa.getAnswer()).append("\n");
                        prompt.append("---\n");
                    });
            prompt.append("\n");
        }
        
        // 4. 添加用户问题
        prompt.append("【用户当前问题】\n");
        prompt.append(question).append("\n\n");
        
        // 5. 添加回答要求
        prompt.append("【回答要求】\n");
        prompt.append("1. 优先使用【最相关问答】中的答案\n");
        prompt.append("2. 如果【最相关问答】不完全匹配，结合【其他相关问答对】推理\n");
        prompt.append("3. 回答格式要清晰，分步骤说明\n");
        prompt.append("4. 如果涉及操作步骤，请标注1、2、3...\n");
        prompt.append("5. 如果是故障排查，请按从易到难的顺序\n");
        prompt.append("6. 如果知识库中完全没有相关信息，请明确告知\n\n");
        
        prompt.append("现在请根据以上知识库内容回答用户的问题：");
        
        return prompt.toString();
    }

    private String formatQAToContext(List<QAPair> qas) {
        if (qas.isEmpty()) {
            return "（无相关问答对）";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(5, qas.size()); i++) {
            QAPair qa = qas.get(i);
            sb.append(i + 1).append(". 问题：").append(qa.getQuestion()).append("\n");
            sb.append("   答案：").append(qa.getAnswer()).append("\n\n");
        }
        return sb.toString();
    }
}