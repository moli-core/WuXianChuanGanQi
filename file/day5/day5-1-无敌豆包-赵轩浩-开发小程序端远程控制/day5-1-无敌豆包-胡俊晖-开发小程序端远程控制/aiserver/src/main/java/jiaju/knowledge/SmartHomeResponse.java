package jiaju.knowledge;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SmartHomeResponse {
    private String question;          // 用户问题
    private String answer;            // 回答内容
    private String deviceType;        // 设备类型
    private long processingTimeMs;    // 处理时间（毫秒）
    private String timestamp;         // 时间戳
    private Integer matchedCount;     // 匹配到的问答对数量
    private String bestMatch;         // 最佳匹配的问题
    private List<String> sources;     // 引用来源
    private Boolean isExactMatch;     // 是否精确匹配
    private String error;             // 错误信息
}
