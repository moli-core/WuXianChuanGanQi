package jiaju.example;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class XunFeiAsrService {

    @Value("${xfyun.app-id}")
    private String appid;

    @Value("${xfyun.api-key}")
    private String apiKey;

    @Value("${xfyun.api-secret}")
    private String apiSecret;

    private static final String HOST_URL = "https://iat-api.xfyun.cn/v2/iat";
    private static final Gson GSON = new Gson();
    private static final int FRAME_SIZE = 1280;
    private static final int INTERVAL = 40;

    public static final int STATUS_FIRST_FRAME = 0;
    public static final int STATUS_CONTINUE_FRAME = 1;
    public static final int STATUS_LAST_FRAME = 2;

    // ==================== Setter 方法（用于测试） ====================
    public void setAppid(String appid) { this.appid = appid; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }

    // ==================== 公开方法 ====================

    /**
     * 识别 MultipartFile 音频（自动选择格式）
     */
    public String audioToText(MultipartFile audio) throws Exception {
        String originalFilename = audio.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase()
                : "";

        log.info("接收音频文件：{}，格式：{}，大小：{} bytes",
                originalFilename, extension, audio.getSize());

        // 保存上传的文件到临时文件 mp3
        File tempFile = File.createTempFile("xfyun_input_", extension);
        audio.transferTo(tempFile);
        log.info("临时文件已保存：{}，大小：{} bytes", tempFile.getAbsolutePath(), tempFile.length());

        try {
            // ========== 统一转换为 PCM 格式 ==========
            log.info("开始转换为PCM格式...");
            File pcmFile = convertToPcm(tempFile);

            try {
                log.info("PCM转换成功，文件大小：{} bytes", pcmFile.length());
                // 使用PCM识别
                return recognizePcmFile(pcmFile.getAbsolutePath());
            } finally {
                // 清理PCM临时文件
                if (pcmFile.exists() && pcmFile.delete()) {
                    log.debug("PCM临时文件已清理");
                }
            }
        } finally {
            // 清理输入临时文件
            if (tempFile.exists() && tempFile.delete()) {
                log.debug("输入临时文件已清理");
            }
        }
    }

    /**
     * 统一转换音频为PCM格式（16kHz/16bit/单声道）
     */
    private File convertToPcm(File inputFile) throws Exception {
        File pcmFile = File.createTempFile("xfyun_pcm_", ".pcm");

        log.info("FFmpeg转换：{} -> {}", inputFile.getAbsolutePath(), pcmFile.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-i", inputFile.getAbsolutePath(),
                "-f", "s16le",           // 16位小端PCM格式
                "-acodec", "pcm_s16le",  // PCM 16位编码
                "-ar", "16000",          // 采样率16kHz
                "-ac", "1",              // 单声道
                "-af", "volume=2.0",     // 音量增益2倍（可选，增强识别率）
                "-y",                    // 覆盖输出文件
                pcmFile.getAbsolutePath()
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // 读取FFmpeg输出（避免进程阻塞）
        try (java.io.InputStream is = process.getInputStream()) {
            byte[] buffer = new byte[1024];
            while (is.read(buffer) != -1) {
                // 只是读取，避免阻塞
            }
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg转换失败，退出码：" + exitCode + "，请检查FFmpeg是否安装");
        }

        if (!pcmFile.exists() || pcmFile.length() == 0) {
            throw new RuntimeException("PCM文件生成失败或为空");
        }

        double duration = pcmFile.length() / 32000.0; // 16kHz/16bit/单声道 = 32000 bytes/秒
        log.info("PCM转换成功，大小：{} bytes，时长约：{} 秒", pcmFile.length(), String.format("%.2f", duration));

        return pcmFile;
    }

    /**
     * 识别文件路径（用于测试）
     */
    public String audioToText(String filePath) throws Exception {
        log.info("识别文件：{}", filePath);
        File file = new File(filePath);
        if (!file.exists()) {
            throw new RuntimeException("文件不存在：" + filePath);
        }
//        String name = file.getName().toLowerCase();
//        if (name.endsWith(".mp3") || name.endsWith(".aac") || name.endsWith(".m4a")) {
//            return recognizeMP3File(filePath);
//        }
        return recognizePcmFile(filePath);
    }

    // ==================== MP3 直接识别（不转换） ====================

    /**
     * 直接识别 MP3（不转换，讯飞原生支持）
     */
    public String audioToTextMP3(MultipartFile audio) throws Exception {
        File tempFile = File.createTempFile("xfyun_", ".mp3");
        audio.transferTo(tempFile);
        log.info("MP3 文件：{}，大小：{} bytes", tempFile.getAbsolutePath(), tempFile.length());
        try {
            return recognizeMP3File(tempFile.getAbsolutePath());
        } finally {
            if (tempFile.exists() && tempFile.delete()) {
                log.debug("临时文件已清理");
            }
        }
    }

    /**
     * 识别 MP3 文件（直接发送，不转换）- 修复版
     */
    private String recognizeMP3File(String filePath) throws Exception {
        File mp3File = new File(filePath);
        if (!mp3File.exists()) {
            throw new RuntimeException("文件不存在：" + filePath);
        }

        long fileSize = mp3File.length();
        log.info("MP3 文件大小：{} bytes", fileSize);

        if (fileSize < 1024) {
            throw new RuntimeException("MP3 文件太小（< 1KB），录音可能无效");
        }

        byte[] audioData = Files.readAllBytes(mp3File.toPath());
        log.info("MP3 数据大小：{} bytes", audioData.length);

        String authUrl = getAuthUrl(HOST_URL, apiKey, apiSecret);
        String wsUrl = authUrl.replace("http://", "ws://").replace("https://", "wss://");

        log.info("WebSocket 连接中...");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> finalResult = new AtomicReference<>("");
        AtomicReference<Exception> error = new AtomicReference<>(null);

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder().url(wsUrl).build();

        WebSocketListener listener = new WebSocketListener() {
            private final Decoder decoder = new Decoder();
            private Date dateBegin = new Date();

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.info("WebSocket 连接已建立");

                new Thread(() -> {
                    try {
                        sendMP3Data(webSocket, audioData);
                    } catch (Exception e) {
                        log.error("发送 MP3 数据失败", e);
                        error.set(e);
                        latch.countDown();
                    }
                }).start();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                log.info("收到响应：{}", text);
                try {
                    ResponseData resp = GSON.fromJson(text, ResponseData.class);
                    if (resp != null) {
                        if (resp.getCode() != 0) {
                            log.error("识别错误，code：{}，message：{}，sid：{}",
                                    resp.getCode(), resp.getMessage(), resp.getSid());
                            error.set(new RuntimeException("识别错误：" + resp.getMessage()));
                            latch.countDown();
                            return;
                        }

                        if (resp.getData() != null && resp.getData().getResult() != null) {
                            Text te = resp.getData().getResult().getText();
                            try {
                                decoder.decode(te);
                                String currentText = decoder.toString();
                                if (currentText != null && !currentText.isEmpty()) {
                                    log.info("中间识别结果：{}", currentText);
                                    finalResult.set(currentText);
                                }
                            } catch (Exception e) {
                                log.error("解码失败", e);
                            }
                        }

                        if (resp.getData() != null && resp.getData().getStatus() == 2) {
                            String result = decoder.toString();
                            if (result != null && !result.isEmpty()) {
                                log.info("最终识别结果：{}", result);
                                finalResult.set(result);
                            } else {
                                log.warn("最终结果为空");
                            }
                            Date dateEnd = new Date();
                            log.info("识别耗时：{}ms", (dateEnd.getTime() - dateBegin.getTime()));
                            log.info("本次识别 sid：{}", resp.getSid());
                            decoder.discard();
                            latch.countDown();
                            webSocket.close(1000, "");
                        }
                    }
                } catch (Exception e) {
                    log.error("处理响应异常", e);
                    error.set(e);
                    latch.countDown();
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("WebSocket 失败", t);
                if (response != null) {
                    log.error("响应码：{}", response.code());
                }
                error.set(new RuntimeException("WebSocket 连接失败", t));
                latch.countDown();
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                log.info("WebSocket 连接关闭，code：{}，reason：{}", code, reason);
                latch.countDown();
            }
        };

        WebSocket webSocket = client.newWebSocket(request, listener);
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        client.dispatcher().executorService().shutdown();

        if (!completed) {
            throw new RuntimeException("语音识别超时（60秒）");
        }

        if (error.get() != null) {
            throw new RuntimeException("识别失败", error.get());
        }

        String result = finalResult.get();
        if (result == null || result.isEmpty()) {
            throw new RuntimeException("未获取到识别结果，请检查：\n" +
                    "1. 音频时长是否在 60 秒以内\n" +
                    "2. 录音内容是否包含有效语音\n" +
                    "3. 音频格式是否为 16kHz/单声道 MP3");
        }

        return result;
    }

    /**
     * 发送 MP3 数据 - 修复版
     */
    private void sendMP3Data(WebSocket webSocket, byte[] audioData) {
        int frameSize = 1280;
        int totalFrames = (audioData.length + frameSize - 1) / frameSize;

        log.info("========== MP3发送开始 ==========");
        log.info("总数据大小: {} bytes", audioData.length);
        log.info("帧大小: {} bytes", frameSize);
        log.info("总帧数: {}", totalFrames);
        log.info("预计发送时长: {} ms", totalFrames * INTERVAL);

        for (int i = 0; i < totalFrames; i++) {
            int start = i * frameSize;
            int end = Math.min(start + frameSize, audioData.length);
            byte[] frame = new byte[end - start];
            System.arraycopy(audioData, start, frame, 0, frame.length);

            JsonObject params = new JsonObject();

            // common参数（每帧都需要）
            JsonObject common = new JsonObject();
            common.addProperty("app_id", appid);
            params.add("common", common);

            // business参数（仅第一帧）
            if (i == 0) {
                JsonObject business = new JsonObject();
                business.addProperty("language", "zh_cn");
                business.addProperty("domain", "iat");
                business.addProperty("accent", "mandarin");
                business.addProperty("dwa", "wpgs");
                business.addProperty("vad_eos", 5000);
                business.addProperty("ptt", 0);
                business.addProperty("nbest", 1);
                params.add("business", business);
            }

            // data参数
            JsonObject data = new JsonObject();
            int frameStatus;
            if (i == 0) {
                frameStatus = STATUS_FIRST_FRAME;  // 第一帧：0
            } else if (i == totalFrames - 1) {
                frameStatus = STATUS_LAST_FRAME;   // 最后一帧：2
            } else {
                frameStatus = STATUS_CONTINUE_FRAME; // 中间帧：1
            }

            data.addProperty("status", frameStatus);
            data.addProperty("format", "audio/mpeg");
            data.addProperty("encoding", "raw");
            data.addProperty("audio", Base64.getEncoder().encodeToString(frame));
            params.add("data", data);

            // 发送帧
            String frameJson = params.toString();
            webSocket.send(frameJson);

            // 日志
            if (i == 0 || i == totalFrames - 1 || i % 5 == 0) {
                log.info("发送帧 {}/{}，status：{}，帧大小：{} bytes，总发送进度：{}/{} bytes",
                        i + 1, totalFrames, frameStatus, frame.length,
                        end, audioData.length);
            }

            // 控制发送速率（40ms一帧）
            if (i < totalFrames - 1) {
                try {
                    Thread.sleep(INTERVAL);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("发送被中断");
                    break;
                }
            }
        }

        log.info("========== MP3发送完成 ==========");
        log.info("最后一帧已发送（status=2），总帧数：{}，总字节：{}", totalFrames, audioData.length);
    }

    // ==================== PCM 识别 ====================

    /**
     * 识别 PCM 文件（带诊断）- 修复版
     */
    private String recognizePcmFile(String filePath) throws Exception {
        File pcmFile = new File(filePath);
        if (!pcmFile.exists()) {
            throw new RuntimeException("文件不存在：" + filePath);
        }

        long fileSize = pcmFile.length();
        log.info("=== PCM 文件诊断 ===");
        log.info("文件路径：{}", filePath);
        log.info("文件大小：{} bytes", fileSize);

        if (fileSize < 3200) {
            throw new RuntimeException("音频太短（< 0.1 秒），文件大小：" + fileSize + " bytes");
        }

        byte[] audioData = Files.readAllBytes(pcmFile.toPath());

        // 统计分析
        int nonZeroCount = 0;
        long sum = 0;
        int maxAmplitude = 0;

        for (int i = 0; i < audioData.length; i++) {
            int value = audioData[i] & 0xFF;
            if (value != 0) {
                nonZeroCount++;
                sum += value;
                if (value > maxAmplitude) maxAmplitude = value;
            }
        }

        double nonZeroRatio = (double) nonZeroCount / audioData.length * 100;
        double avgAmplitude = nonZeroCount > 0 ? (double) sum / nonZeroCount : 0;

        log.info("=== 音频数据分析 ===");
        log.info("非零字节数：{} ({:.2f}%)", nonZeroCount, nonZeroRatio);
        log.info("平均振幅：{:.2f}", avgAmplitude);
        log.info("最大振幅：{}", maxAmplitude);

        if (nonZeroRatio < 1.0) {
            throw new RuntimeException("音频数据几乎全为 0（静音），非零比例：" +
                    String.format("%.2f", nonZeroRatio) + "%");
        }

        String authUrl = getAuthUrl(HOST_URL, apiKey, apiSecret);
        String wsUrl = authUrl.replace("http://", "ws://").replace("https://", "wss://");

        log.info("WebSocket 连接中...");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> finalResult = new AtomicReference<>("");
        AtomicReference<Exception> error = new AtomicReference<>(null);

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder().url(wsUrl).build();

        WebSocketListener listener = new WebSocketListener() {
            private final Decoder decoder = new Decoder();
            private Date dateBegin = new Date();

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.info("WebSocket 连接已建立");

                new Thread(() -> {
                    try {
                        sendPcmData(webSocket, filePath);
                    } catch (Exception e) {
                        log.error("发送 PCM 数据失败", e);
                        error.set(e);
                        latch.countDown();
                    }
                }).start();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                log.info("收到响应：{}", text);
                try {
                    ResponseData resp = GSON.fromJson(text, ResponseData.class);
                    if (resp != null) {
                        if (resp.getCode() != 0) {
                            log.error("识别错误，code：{}，message：{}，sid：{}",
                                    resp.getCode(), resp.getMessage(), resp.getSid());
                            error.set(new RuntimeException("识别错误：" + resp.getMessage()));
                            latch.countDown();
                            return;
                        }

                        if (resp.getData() != null && resp.getData().getResult() != null) {
                            Text te = resp.getData().getResult().getText();
                            try {
                                decoder.decode(te);
                                String currentText = decoder.toString();
                                if (currentText != null && !currentText.isEmpty()) {
                                    log.info("中间识别结果：{}", currentText);
                                    finalResult.set(currentText);
                                }
                            } catch (Exception e) {
                                log.error("解码失败", e);
                            }
                        }

                        if (resp.getData() != null && resp.getData().getStatus() == 2) {
                            String result = decoder.toString();
                            if (result != null && !result.isEmpty()) {
                                log.info("最终识别结果：{}", result);
                                finalResult.set(result);
                            } else {
                                log.warn("最终结果为空");
                            }
                            Date dateEnd = new Date();
                            log.info("识别耗时：{}ms", (dateEnd.getTime() - dateBegin.getTime()));
                            log.info("本次识别 sid：{}", resp.getSid());
                            decoder.discard();
                            latch.countDown();
                            webSocket.close(1000, "");
                        }
                    }
                } catch (Exception e) {
                    log.error("处理响应异常", e);
                    error.set(e);
                    latch.countDown();
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("WebSocket 失败", t);
                if (response != null) {
                    log.error("响应码：{}", response.code());
                }
                error.set(new RuntimeException("WebSocket 连接失败", t));
                latch.countDown();
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                log.info("WebSocket 连接关闭，code：{}，reason：{}", code, reason);
                latch.countDown();
            }
        };

        WebSocket webSocket = client.newWebSocket(request, listener);
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        client.dispatcher().executorService().shutdown();

        if (!completed) {
            throw new RuntimeException("语音识别超时（60秒）");
        }

        if (error.get() != null) {
            throw new RuntimeException("识别失败", error.get());
        }

        String result = finalResult.get();
        if (result == null || result.isEmpty()) {
            throw new RuntimeException("未获取到识别结果，请检查：\n" +
                    "1. 音频格式是否为 16kHz/16bit/单声道/PCM\n" +
                    "2. 录音时长是否在 60 秒以内\n" +
                    "3. 录音内容是否包含有效语音");
        }

        return result;
    }

    /**
     * 发送 PCM 数据 - 修复版
     */
    private void sendPcmData(WebSocket webSocket, String filePath) {
        int frameCount = 0;

        try (FileInputStream fs = new FileInputStream(filePath)) {
            byte[] buffer = new byte[FRAME_SIZE];
            long totalBytes = new File(filePath).length();
            long sentBytes = 0;

            log.info("========== PCM发送开始 ==========");
            log.info("文件大小: {} bytes", totalBytes);
            log.info("帧大小: {} bytes", FRAME_SIZE);

            while (true) {
                int len = fs.read(buffer);
                boolean isLastFrame = (len == -1);

                JsonObject frame = new JsonObject();

                // common参数（每帧都需要）
                JsonObject common = new JsonObject();
                common.addProperty("app_id", appid);
                frame.add("common", common);

                // business参数（仅第一帧）
                if (frameCount == 0) {
                    JsonObject business = new JsonObject();
                    business.addProperty("language", "zh_cn");
                    business.addProperty("domain", "iat");
                    business.addProperty("accent", "mandarin");
                    business.addProperty("dwa", "wpgs");
                    business.addProperty("vad_eos", 5000);
                    business.addProperty("ptt", 0);
                    business.addProperty("nbest", 1);
                    frame.add("business", business);
                }

                // data参数
                JsonObject data = new JsonObject();
                if (isLastFrame) {
                    // 最后一帧：status=2，audio为空
                    data.addProperty("status", STATUS_LAST_FRAME);
                    data.addProperty("format", "audio/L16;rate=16000");
                    data.addProperty("encoding", "raw");
                    data.addProperty("audio", "");
                    frame.add("data", data);

                    webSocket.send(frame.toString());
                    frameCount++;
                    log.info("结束帧已发送（status=2），总帧数：{}，总发送：{} bytes", frameCount, sentBytes);
                    break;
                } else if (frameCount == 0) {
                    // 第一帧：status=0
                    data.addProperty("status", STATUS_FIRST_FRAME);
                    data.addProperty("format", "audio/L16;rate=16000");
                    data.addProperty("encoding", "raw");
                    data.addProperty("audio", Base64.getEncoder().encodeToString(
                            Arrays.copyOf(buffer, len)));
                    frame.add("data", data);
                } else {
                    // 中间帧：status=1
                    data.addProperty("status", STATUS_CONTINUE_FRAME);
                    data.addProperty("format", "audio/L16;rate=16000");
                    data.addProperty("encoding", "raw");
                    data.addProperty("audio", Base64.getEncoder().encodeToString(
                            Arrays.copyOf(buffer, len)));
                    frame.add("data", data);
                }

                webSocket.send(frame.toString());
                frameCount++;
                sentBytes += len;

                // 日志
                if (frameCount == 1 || frameCount % 10 == 0) {
                    log.info("发送帧 {}，status：{}，帧大小：{} bytes，总发送：{}/{} bytes",
                            frameCount, frameCount == 1 ? 0 : 1, len, sentBytes, totalBytes);
                }

                try {
                    Thread.sleep(INTERVAL);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("发送被中断");
                    break;
                }
            }

            log.info("========== PCM发送完成 ==========");
            log.info("总帧数：{}，总字节：{}", frameCount, sentBytes);

        } catch (IOException e) {
            log.error("发送 PCM 数据失败", e);
        }
    }

    // ==================== FFmpeg 转换（备用） ====================



    // ==================== 鉴权工具 ====================

    private String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());

        StringBuilder builder = new StringBuilder("host: ").append(url.getHost()).append("\n")
                .append("date: ").append(date).append("\n")
                .append("GET ").append(url.getPath()).append(" HTTP/1.1");

        Charset charset = Charset.forName("UTF-8");
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(builder.toString().getBytes(charset));
        String sha = Base64.getEncoder().encodeToString(hexDigits);

        String authorization = String.format(
                "api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                apiKey, "hmac-sha256", "host date request-line", sha);

        HttpUrl httpUrl = HttpUrl.parse("https://" + url.getHost() + url.getPath())
                .newBuilder()
                .addQueryParameter("authorization", Base64.getEncoder().encodeToString(
                        authorization.getBytes(charset)))
                .addQueryParameter("date", date)
                .addQueryParameter("host", url.getHost())
                .build();

        return httpUrl.toString();
    }

    // ==================== 内部数据类 ====================

    public static class ResponseData {
        private int code;
        private String message;
        private String sid;
        private Data data;
        public int getCode() { return code; }
        public String getMessage() { return message; }
        public String getSid() { return sid; }
        public Data getData() { return data; }
    }

    public static class Data {
        private int status;
        private Result result;
        public int getStatus() { return status; }
        public Result getResult() { return result; }
    }

    public static class Result {
        int bg;
        int ed;
        String pgs;
        int[] rg;
        int sn;
        Ws[] ws;
        boolean ls;
        JsonObject vad;

        public Text getText() {
            Text text = new Text();
            StringBuilder sb = new StringBuilder();
            if (ws != null) {
                for (Ws w : ws) {
                    if (w.cw != null && w.cw.length > 0) {
                        sb.append(w.cw[0].w);
                    }
                }
            }
            text.sn = this.sn;
            text.text = sb.toString();
            text.rg = this.rg;
            text.pgs = this.pgs;
            text.bg = this.bg;
            text.ed = this.ed;
            text.ls = this.ls;
            text.vad = this.vad == null ? null : this.vad;
            return text;
        }
    }

    public static class Ws {
        Cw[] cw;
        int bg;
        int ed;
    }

    public static class Cw {
        int sc;
        String w;
    }

    public static class Text {
        int sn;
        int bg;
        int ed;
        String text;
        String pgs;
        int[] rg;
        boolean deleted;
        boolean ls;
        JsonObject vad;
    }

    public static class Decoder {
        private Text[] texts;
        private int defc = 10;

        public Decoder() {
            this.texts = new Text[this.defc];
        }

        public synchronized void decode(Text text) {
            if (text.sn >= this.defc) {
                this.resize();
            }
            if ("rpl".equals(text.pgs)) {
                for (int i = text.rg[0]; i <= text.rg[1]; i++) {
                    if (i < this.texts.length) {
                        this.texts[i].deleted = true;
                    }
                }
            }
            this.texts[text.sn] = text;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Text t : this.texts) {
                if (t != null && !t.deleted) {
                    sb.append(t.text);
                }
            }
            return sb.toString();
        }

        public void resize() {
            int oc = this.defc;
            this.defc <<= 1;
            Text[] old = this.texts;
            this.texts = new Text[this.defc];
            for (int i = 0; i < oc; i++) {
                this.texts[i] = old[i];
            }
        }

        public void discard() {
            for (int i = 0; i < this.texts.length; i++) {
                this.texts[i] = null;
            }
        }
    }
}