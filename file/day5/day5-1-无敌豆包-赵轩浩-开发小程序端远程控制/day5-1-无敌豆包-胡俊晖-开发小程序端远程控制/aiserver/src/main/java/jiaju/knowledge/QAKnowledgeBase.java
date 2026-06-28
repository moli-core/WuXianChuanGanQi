package jiaju.knowledge;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class QAKnowledgeBase {

    // 存储所有问答对
    private final List<QAPair> qaPairs = new ArrayList<>();

    // 按设备类型索引
    private final Map<String, List<QAPair>> deviceTypeIndex = new HashMap<>();

    // 按关键词索引
    private final Map<String, List<QAPair>> keywordIndex = new HashMap<>();

    @PostConstruct
    public void init() {
        // 加载智能家居问答数据
        loadSmartHomeQAData();

        // 构建索引
        buildIndexes();

        System.out.println("知识库初始化完成，共加载 " + qaPairs.size() + " 条问答对");
    }

    private void loadSmartHomeQAData() {
        // 智能灯泡相关
        qaPairs.add(new QAPair("照明设备",
                "如何连接智能灯泡到WiFi？",
                "连接步骤：\n1. 确保灯泡已通电并处于配对模式（快速开关3次，灯泡闪烁）\n2. 打开智能家居APP，点击添加设备\n3. APP会自动搜索附近的灯泡\n4. 选择WiFi网络并输入密码\n5. 等待连接成功（约30秒），灯泡常亮即表示连接成功",
                Arrays.asList("连接", "WiFi", "配对", "灯泡")
        ));

        qaPairs.add(new QAPair("照明设备",
                "智能灯泡如何调节色温？",
                "色温调节方法：\n1. 在APP中点击已连接的灯泡\n2. 找到色温调节滑块（或色温模式）\n3. 向左滑动为暖色（2700K-3000K），适合温馨氛围\n4. 向右滑动为冷色（5000K-6500K），适合工作阅读\n5. 部分型号支持预设场景模式（阅读、睡眠、聚会等）",
                Arrays.asList("色温", "调节", "亮度", "灯泡")
        ));

        qaPairs.add(new QAPair("照明设备",
                "智能灯泡无法连接到WiFi怎么办？",
                "故障排查步骤：\n1. 检查灯泡是否处于配对模式（快速开关3次）\n2. 确认WiFi密码是否正确\n3. 确认WiFi是2.4GHz（不支持5GHz）\n4. 重启路由器和灯泡\n5. 检查APP是否为最新版本\n6. 如果以上无效，长按灯泡10秒恢复出厂设置",
                Arrays.asList("故障", "WiFi", "连接", "灯泡")
        ));

        // 智能插座相关
        qaPairs.add(new QAPair("智能插座",
                "智能插座如何设置定时开关？",
                "定时设置方法：\n1. 在APP中添加智能插座并连接\n2. 进入插座控制界面，点击定时功能\n3. 选择添加定时规则\n4. 设置开启时间和关闭时间\n5. 选择重复周期（每天/工作日/周末/自定义）\n6. 保存设置，插座将按规则自动开关",
                Arrays.asList("定时", "开关", "插座", "计划")
        ));

        qaPairs.add(new QAPair("智能插座",
                "智能插座不通电是什么原因？",
                "故障排查：\n1. 检查插座是否插入正常电源口\n2. 确认插座上的开关是否开启（指示灯亮）\n3. 检查APP中插座是否在线\n4. 尝试长按插座侧面的重置键5秒\n5. 检查是否过载（超过额定功率）\n6. 如仍无效，可能是硬件故障，联系售后",
                Arrays.asList("故障", "不通电", "插座", "电源")
        ));

        // 智能网关相关
        qaPairs.add(new QAPair("智能网关",
                "如何设置智能场景联动？",
                "场景联动设置步骤：\n1. 确保所有设备已通过网关连接\n2. 在APP中进入场景/自动化页面\n3. 点击创建新场景\n4. 设置触发条件（如：定时、传感器触发、手动触发）\n5. 设置执行动作（如：开灯、关窗帘、播放音乐）\n6. 保存场景并测试\n7. 支持多条件组合（AND/OR逻辑）",
                Arrays.asList("场景", "联动", "自动化", "网关")
        ));

        qaPairs.add(new QAPair("智能网关",
                "网关离线怎么办？",
                "网关离线处理：\n1. 检查网关电源和网线（有线连接）或WiFi信号（无线连接）\n2. 确认路由器工作正常\n3. 重启网关（拔插电源）\n4. 检查网关固件是否需要更新\n5. 如果使用有线连接，换一根网线测试\n6. 使用APP重新配网\n7. 若仍离线，联系客服获取技术支持",
                Arrays.asList("离线", "网关", "断网", "连接")
        ));

        // 通用问题
        qaPairs.add(new QAPair("通用",
                "智能家居设备如何恢复出厂设置？",
                "通用恢复出厂设置方法：\n1. 不同设备恢复方式不同，请参考：\n   - 智能灯泡：快速开关灯5次\n   - 智能插座：长按重置键10秒\n   - 智能网关：长按复位键10秒或使用复位针\n   - 智能摄像头：长按重置孔10秒\n2. 恢复后需要重新配网\n3. 注意：恢复出厂设置会清除所有个性化设置",
                Arrays.asList("恢复出厂", "重置", "初始化")
        ));

        qaPairs.add(new QAPair("通用",
                "智能家居设备如何节能？",
                "节能建议：\n1. 合理设置定时开关，避免长时间待机\n2. 使用场景联动，人走灯灭\n3. 智能插座可监测用电量，发现高耗电设备\n4. 选择能效等级高的智能设备\n5. 设置亮度自动调节（根据环境光）\n6. 关闭不常用设备的后台刷新",
                Arrays.asList("节能", "省电", "功耗")
        ));
    }

    private void buildIndexes() {
        // 按设备类型建立索引
        for (QAPair qa : qaPairs) {
            String deviceType = qa.getDeviceType();
            deviceTypeIndex.computeIfAbsent(deviceType, k -> new ArrayList<>()).add(qa);

            // 提取关键词建立索引
            for (String tag : qa.getTags()) {
                keywordIndex.computeIfAbsent(tag, k -> new ArrayList<>()).add(qa);
            }
        }
    }

    // ========== 查询方法 ==========

    /**
     * 根据设备类型查找问答对
     */
    public List<QAPair> findByDeviceType(String deviceType) {
        if (deviceType == null || deviceType.isEmpty()) {
            return getAllQAPairs();
        }
        return deviceTypeIndex.getOrDefault(deviceType, new ArrayList<>());
    }

    /**
     * 精确匹配问答
     */
    public QAPair findExactMatch(String question) {
        return qaPairs.stream()
                .filter(qa -> qa.getQuestion().contains(question) || question.contains(qa.getQuestion()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 模糊匹配（基于关键词）
     */
    public List<QAPair> findFuzzyMatch(String question) {
        // 提取问题中的关键词
        String[] words = question.replaceAll("[，,、。.？?！!]", " ").split("\\s+");
        Set<String> keywords = Arrays.stream(words)
                .filter(w -> w.length() >= 2)
                .collect(Collectors.toSet());

        Set<QAPair> results = new HashSet<>();

        for (String keyword : keywords) {
            List<QAPair> matches = keywordIndex.getOrDefault(keyword, new ArrayList<>());
            results.addAll(matches);
        }

        // 按匹配度排序（匹配关键词数量）
        return results.stream()
                .sorted((a, b) -> {
                    long aMatches = countMatches(a, keywords);
                    long bMatches = countMatches(b, keywords);
                    return Long.compare(bMatches, aMatches);
                })
                .collect(Collectors.toList());
    }

    /**
     * 综合搜索（设备类型 + 关键词）
     */
    public List<QAPair> search(String question, String deviceType) {
        // 1. 先按设备类型筛选
        List<QAPair> filtered = findByDeviceType(deviceType);

        // 2. 再按关键词匹配
        String[] keywords = question.replaceAll("[，,、。.？?！!]", " ").split("\\s+");
        Set<String> keywordSet = Arrays.stream(keywords)
                .filter(w -> w.length() >= 2)
                .collect(Collectors.toSet());

        return filtered.stream()
                .filter(qa -> {
                    for (String keyword : keywordSet) {
                        if (qa.getQuestion().contains(keyword) ||
                                qa.getAnswer().contains(keyword) ||
                                qa.getTags().stream().anyMatch(tag -> tag.contains(keyword))) {
                            return true;
                        }
                    }
                    return false;
                })
                .sorted((a, b) -> {
                    long aMatches = countMatches(a, keywordSet);
                    long bMatches = countMatches(b, keywordSet);
                    return Long.compare(bMatches, aMatches);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取所有问答对
     */
    public List<QAPair> getAllQAPairs() {
        return new ArrayList<>(qaPairs);
    }

    /**
     * 获取设备类型列表
     */
    public List<String> getAllDeviceTypes() {
        return new ArrayList<>(deviceTypeIndex.keySet());
    }

    // ========== 私有辅助方法 ==========

    private long countMatches(QAPair qa, Set<String> keywords) {
        long count = 0;
        for (String keyword : keywords) {
            if (qa.getQuestion().contains(keyword)) count += 2;
            if (qa.getAnswer().contains(keyword)) count += 1;
            if (qa.getTags().stream().anyMatch(tag -> tag.contains(keyword))) count += 1;
        }
        return count;
    }

    private long countMatches(QAPair qa, String[] keywords) {
        Set<String> keywordSet = new HashSet<>(Arrays.asList(keywords));
        return countMatches(qa, keywordSet);
    }
}

// 问答对实体类
class QAPair {
    private String deviceType;
    private String question;
    private String answer;
    private List<String> tags;

    public QAPair(String deviceType, String question, String answer, List<String> tags) {
        this.deviceType = deviceType;
        this.question = question;
        this.answer = answer;
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public QAPair(String deviceType, String question, String answer) {
        this(deviceType, question, answer, new ArrayList<>());
    }

    // Getters and Setters
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}