package jiaju.example;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "xfyun")
public class XunFeiConfig {
    private String appId;
    private String apiKey;
    private String apiSecret;
    private Audio audio = new Audio();

    @Data
    public static class Audio {
        private int sampleRate = 16000;
        private String format = "pcm";
    }
}