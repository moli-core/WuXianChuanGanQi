package jiaju.knowledge;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SmartHomeRequest {
    private String question;          // 用户问题
    private String deviceType;        // 设备类型（可选）
    private String sessionId;         // 会话ID（用于多轮对话）
    private Double threshold;         // 匹配阈值
    private Integer topK;             // 返回数量
}

