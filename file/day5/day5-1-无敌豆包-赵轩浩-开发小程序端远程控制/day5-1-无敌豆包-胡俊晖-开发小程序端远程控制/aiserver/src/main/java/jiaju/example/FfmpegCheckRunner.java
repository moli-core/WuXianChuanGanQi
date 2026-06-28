package jiaju.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FfmpegCheckRunner implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        boolean available = AudioFormatConverter.isFfmpegAvailable();
        if (available) {
            log.info("✅ FFmpeg 检测成功，语音识别功能可用");
        } else {
            log.error("❌ FFmpeg 未检测到，请安装 FFmpeg 并添加到环境变量");
            log.error("   下载地址：https://ffmpeg.org/download.html");
            log.error("   Windows 用户：https://www.gyan.dev/ffmpeg/builds/");
        }
    }
}