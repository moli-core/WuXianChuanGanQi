package jiaju.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;

@Slf4j
public class AudioFormatConverter {

    /**
     * 增强版转换：针对科大讯飞优化
     */
    public static File convertToXunfeiFormat(MultipartFile audio) throws Exception {
        String originalFilename = audio.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase()
                : ".mp3";

        log.info("开始转换音频：{}，格式：{}，大小：{} bytes",
                originalFilename, extension, audio.getSize());

        File debugDir = new File("D:/audio_debug/");
        if (!debugDir.exists()) debugDir.mkdirs();

        String timestamp = String.valueOf(System.currentTimeMillis());
        File originalFile = new File(debugDir, "original_" + timestamp + extension);
        audio.transferTo(originalFile);
        log.info("原始文件已保存：{}，大小：{} bytes", originalFile.getAbsolutePath(), originalFile.length());

        // 创建临时文件
        File inputFile = File.createTempFile("audio_input_", extension);
        audio.transferTo(inputFile);

        // 输出文件
        File outputFile = new File(debugDir, "converted_" + timestamp + ".mp3");

        try {
            // ========== FFmpeg 增强处理 ==========
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-i", inputFile.getAbsolutePath(),

                    // 音频滤镜链：增强语音质量
                    "-af",
                    // 多个滤镜用逗号连接
                    "highpass=f=200," +          // 高通滤波，去除低频噪音（200Hz以下）
                            "lowpass=f=4000," +           // 低通滤波，保留语音频段（4000Hz以下）
                            "volume=4.0," +               // 增大音量4倍（关键！）
                            "silenceremove=1:0:-50dB," +  // 去除开头和结尾的静音
                            "dynaudnorm=f=500:g=15",      // 动态音频归一化

                    // 音频编码参数
                    "-acodec", "libmp3lame",
                    "-ar", "16000",               // 16kHz采样率
                    "-ac", "1",                   // 单声道
                    "-ab", "128k",                // 提高比特率到128kbps（更好质量）
                    "-y",
                    outputFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);

            log.info("执行 FFmpeg 命令：{}", String.join(" ", pb.command()));

            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug("FFmpeg: {}", line);
                }
            }

            int exitCode = process.waitFor();
            log.info("FFmpeg 退出码：{}", exitCode);

            if (exitCode != 0) {
                log.error("FFmpeg 转换失败：\n{}", output);
                throw new RuntimeException("FFmpeg MP3转换失败，退出码：" + exitCode);
            }

            // 验证输出文件
            if (!outputFile.exists() || outputFile.length() == 0) {
                throw new RuntimeException("MP3 文件生成失败或为空");
            }

            validateMp3File(outputFile);

            // ========== 额外调试：生成一个放大音量的版本用于对比 ==========
            File boostedFile = new File(debugDir, "boosted_" + timestamp + ".mp3");
            generateBoostedVersion(inputFile, boostedFile);

            log.info("转换完成！");
            log.info("  原始文件：{} ({} bytes)", originalFile.getAbsolutePath(), originalFile.length());
            log.info("  增强文件：{} ({} bytes)", outputFile.getAbsolutePath(), outputFile.length());
            log.info("  超高增益：{} ({} bytes)", boostedFile.getAbsolutePath(), boostedFile.length());

            return outputFile;

        } catch (Exception e) {
            if (e.getMessage() != null &&
                    (e.getMessage().contains("Cannot run program") ||
                            e.getMessage().contains("error=2"))) {
                throw new RuntimeException(
                        "FFmpeg 未安装或未添加到环境变量\n" +
                                "下载地址：https://ffmpeg.org/download.html\n" +
                                "Windows 用户：https://www.gyan.dev/ffmpeg/builds/",
                        e
                );
            }
            throw e;
        } finally {
            if (inputFile.exists()) inputFile.delete();
        }
    }

    /**
     * 生成超高增益版本用于测试
     */
    private static void generateBoostedVersion(File inputFile, File outputFile) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-i", inputFile.getAbsolutePath(),
                "-af", "volume=10.0,dynaudnorm",  // 10倍增益 + 动态归一化
                "-acodec", "libmp3lame",
                "-ar", "16000",
                "-ac", "1",
                "-ab", "128k",
                "-y",
                outputFile.getAbsolutePath()
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();
        // 读取输出流避免阻塞
        try (InputStream is = process.getInputStream()) {
            byte[] buffer = new byte[1024];
            while (is.read(buffer) != -1) {}
        }
        process.waitFor();

        if (outputFile.exists() && outputFile.length() > 0) {
            log.info("超高增益版本已生成：{}", outputFile.getAbsolutePath());
        }
    }

    /**
     * 分析音频文件信息
     */
    public static void analyzeAudio(File audioFile) throws Exception {
        log.info("========== 音频文件分析 ==========");
        log.info("文件：{}", audioFile.getAbsolutePath());
        log.info("大小：{} bytes", audioFile.length());

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-i", audioFile.getAbsolutePath(),
                "-af", "volumedetect",     // 音量检测
                "-f", "null",
                "-"
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                // 只记录关键信息
                if (line.contains("Stream") ||
                        line.contains("Duration") ||
                        line.contains("mean_volume") ||
                        line.contains("max_volume")) {
                    log.info("  {}", line.trim());
                }
            }
        }
        process.waitFor();
        log.info("========== 分析完成 ==========");
    }

    /**
     * 验证MP3文件
     */
    private static void validateMp3File(File mp3File) throws Exception {
        log.info("MP3 文件大小：{} bytes", mp3File.length());

        // 估算时长（按128kbps计算）
        double duration = (mp3File.length() * 8.0) / 128000.0;
        log.info("音频时长约：{} 秒", String.format("%.2f", duration));

        if (duration < 0.5) {
            throw new RuntimeException("音频时长太短（< 0.5秒），可能无效");
        }
        if (duration > 60) {
            log.warn("⚠️ 音频时长超过60秒（{}），可能超出限制", String.format("%.1f", duration));
        }

        // 分析音频信息
        analyzeAudio(mp3File);
    }

    /**
     * 检查 FFmpeg 是否可用
     */
    public static boolean isFfmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}