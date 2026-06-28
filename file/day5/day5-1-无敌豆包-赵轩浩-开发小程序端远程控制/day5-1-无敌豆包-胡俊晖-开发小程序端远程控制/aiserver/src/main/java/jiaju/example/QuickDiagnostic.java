package jiaju.example;

import java.io.File;
import java.io.FileInputStream;

public class QuickDiagnostic {
    public static void main(String[] args) throws Exception {
        // ========== 诊断1：检查音频是否真的有效 ==========
        String filePath = "D:\\audio_debug\\original_1782320580641.mp3";
        File audioFile = new File(filePath);
        
        System.out.println("========== 音频文件诊断 ==========");
        System.out.println("文件路径: " + filePath);
        System.out.println("文件存在: " + audioFile.exists());
        System.out.println("文件大小: " + audioFile.length() + " bytes");
        
        // 检查MP3文件头
        try (FileInputStream fis = new FileInputStream(audioFile)) {
            byte[] header = new byte[10];
            int read = fis.read(header);
            System.out.println("\n文件头字节(Hex):");
            for (int i = 0; i < read; i++) {
                System.out.printf("%02X ", header[i]);
            }
            System.out.println();
            
            // 检查ID3标签
            if (read >= 3) {
                if (header[0] == 'I' && header[1] == 'D' && header[2] == '3') {
                    System.out.println("✅ 检测到ID3v2标签");
                    // ID3v2头结构
                    if (read >= 10) {
                        int size = ((header[6] & 0x7F) << 21) 
                                 | ((header[7] & 0x7F) << 14)
                                 | ((header[8] & 0x7F) << 7)
                                 | (header[9] & 0x7F);
                        System.out.println("ID3v2标签大小: " + size + " bytes");
                        System.out.println("实际音频数据开始于: " + (10 + size) + " bytes");
                    }
                } else if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xE0) == 0xE0) {
                    System.out.println("✅ 检测到MP3帧同步");
                } else {
                    System.out.println("❌ 未检测到有效的MP3文件头");
                }
            }
        }
        
        // ========== 诊断2：使用FFprobe获取详细信息 ==========
        System.out.println("\n========== FFprobe 详细信息 ==========");
        ProcessBuilder pb = new ProcessBuilder(
                "ffprobe",
                "-v", "quiet",
                "-print_format", "json",
                "-show_format",
                "-show_streams",
                filePath
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        StringBuilder output = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        process.waitFor();
        
        String probeResult = output.toString();
        System.out.println(probeResult);
        
        // 解析关键信息
        if (probeResult.contains("\"codec_type\"")) {
            System.out.println("\n--- 关键信息 ---");
            if (probeResult.contains("\"sample_rate\"")) {
                System.out.print("采样率: ");
                if (probeResult.contains("\"16000\"")) {
                    System.out.println("✅ 16000 Hz");
                } else {
                    System.out.println("❌ 不是16000 Hz");
                }
            }
            if (probeResult.contains("\"channels\"")) {
                System.out.print("声道数: ");
                if (probeResult.contains("\"channels\": 1")) {
                    System.out.println("✅ 单声道");
                } else {
                    System.out.println("❌ 不是单声道");
                }
            }
            if (probeResult.contains("\"codec_name\"")) {
                System.out.print("编码格式: ");
                if (probeResult.contains("\"mp3\"")) {
                    System.out.println("✅ MP3");
                } else {
                    System.out.println("❌ 不是MP3");
                }
            }
            if (probeResult.contains("\"duration\"")) {
                // 提取duration值
                int idx = probeResult.indexOf("\"duration\"");
                if (idx > 0) {
                    int start = probeResult.indexOf("\"", idx + 10) + 1;
                    int end = probeResult.indexOf("\"", start);
                    if (start > 0 && end > start) {
                        String duration = probeResult.substring(start, end);
                        double dur = Double.parseDouble(duration);
                        System.out.printf("时长: %.2f 秒 %s\n", dur, dur <= 60 ? "✅" : "❌ 超过60秒");
                    }
                }
            }
        }
        
        // ========== 诊断3：测试小段音频 ==========
        System.out.println("\n========== 生成标准测试音频 ==========");
        generateAndTestStandardAudio();
    }
    
    private static void generateAndTestStandardAudio() throws Exception {
        File testFile = new File("D:\\audio_debug\\standard_test.mp3");
        
        // 生成一个包含音频的测试文件：生成文本到语音的音频
        // 这里我们生成一个有声音的音频（1000Hz正弦波，持续3秒）
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-f", "lavfi",
                "-i", "sine=frequency=1000:duration=3",
                "-acodec", "libmp3lame",
                "-ar", "16000",
                "-ac", "1",
                "-ab", "128k",
                "-y",
                testFile.getAbsolutePath()
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor();
        
        if (testFile.exists()) {
            System.out.println("✅ 标准测试音频已生成: " + testFile.getAbsolutePath());
            System.out.println("   文件大小: " + testFile.length() + " bytes");
            System.out.println("   （这是1000Hz正弦波，应该能识别出声音但不一定是文字）");
            
            // 你可以用这个文件测试API是否能检测到音频
            System.out.println("\n请用此文件测试识别API: " + testFile.getAbsolutePath());
        }
    }
}