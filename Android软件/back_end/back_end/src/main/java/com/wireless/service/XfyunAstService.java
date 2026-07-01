package com.wireless.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 讯飞 AST 语音识别服务
 * 将 PCM/WAV 音频转为文字 (与硬件同学统一)
 */
@Slf4j
@Service
public class XfyunAstService {

    private static final String DEFAULT_ENDPOINT = "wss://office-api-ast-dx.iflyaisol.com/ast/communicate/v1";
    private static final int CHUNK_SIZE = 1280;
    private static final DateTimeFormatter UTC_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final String END_FLAG = "{\"end\":true}";

    private final HttpClient httpClient;
    private final String appId;
    private final String apiKey;
    private final String apiSecret;
    private final String endpoint;

    public XfyunAstService(@Value("${xfyun.ast.app-id}") String appId,
                           @Value("${xfyun.ast.api-key}") String apiKey,
                           @Value("${xfyun.ast.api-secret}") String apiSecret,
                           @Value("${xfyun.ast.endpoint}") String endpoint) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.appId = appId;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.endpoint = (endpoint == null || endpoint.isBlank()) ? DEFAULT_ENDPOINT : endpoint.trim();
    }

    /**
     * 语音转文字
     * @param audio PCM 16k 16bit 单声道字节
     */
    public String audioToText(byte[] audio, int sampleRate) throws Exception {
        if (audio == null || audio.length == 0) {
            throw new IllegalArgumentException("音频数据为空");
        }

        // 如果音频是 WAV，自动去除头部
        byte[] pcm = audio;
        if (isWav(audio)) {
            pcm = stripWav(audio);
            sampleRate = getWavSampleRate(audio);
        }

        String sessionId = UUID.randomUUID().toString();
        SpeechListener listener = new SpeechListener();
        String url = authenticatedUrl(sampleRate, sessionId);

        WebSocket ws = httpClient.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .buildAsync(URI.create(url), listener)
                .join();

        // 分块发送 PCM
        final byte[] finalPcm = pcm;
        int offset = 0;
        while (offset < finalPcm.length) {
            int size = Math.min(CHUNK_SIZE, finalPcm.length - offset);
            ws.sendBinary(ByteBuffer.wrap(finalPcm, offset, size), true).join();
            offset += size;
            Thread.sleep(40);
        }
        // 发送结束标志
        ws.sendText(JSON.toJSONString(Map.of("end", true, "sessionId", sessionId)), true).join();

        if (!listener.await(60, TimeUnit.SECONDS)) {
            ws.abort();
            throw new Exception("讯飞 AST 超时");
        }
        if (listener.error.get() != null) {
            throw new Exception("讯飞 AST 失败: " + listener.error.get().getMessage());
        }
        String text = listener.text.toString().trim();
        if (text.isEmpty()) {
            throw new Exception("讯飞 AST 返回空文本");
        }
        log.info("讯飞 AST 识别结果: {}", text);
        return text;
    }

    /** 处理 WAV/PCM 自动识别 */
    public String audioToText(byte[] audio) throws Exception {
        int sr = isWav(audio) ? getWavSampleRate(audio) : 16000;
        return audioToText(audio, sr);
    }

    // ===== WAV 处理 =====

    private boolean isWav(byte[] data) {
        return data.length >= 12 &&
                new String(data, 0, 4, StandardCharsets.US_ASCII).equals("RIFF") &&
                new String(data, 8, 4, StandardCharsets.US_ASCII).equals("WAVE");
    }

    private int getWavSampleRate(byte[] wav) {
        return littleEndianInt(wav, 24);
    }

    private byte[] stripWav(byte[] wav) {
        int cursor = 12;
        while (cursor + 8 <= wav.length) {
            String id = new String(wav, cursor, 4, StandardCharsets.US_ASCII);
            int size = littleEndianInt(wav, cursor + 4);
            if ("data".equals(id)) {
                int end = Math.min(cursor + 8 + size, wav.length);
                return Arrays.copyOfRange(wav, cursor + 8, end);
            }
            cursor = cursor + 8 + size + (size % 2);
        }
        throw new IllegalArgumentException("WAV 文件无 data chunk");
    }

    private int littleEndianInt(byte[] b, int off) {
        return (b[off] & 0xff) | ((b[off + 1] & 0xff) << 8) |
                ((b[off + 2] & 0xff) << 16) | ((b[off + 3] & 0xff) << 24);
    }

    // ===== 讯飞认证 URL =====

    private String authenticatedUrl(int sampleRate, String sessionId) {
        Map<String, String> params = new TreeMap<>();
        params.put("accessKeyId", apiKey);
        params.put("appId", appId);
        params.put("audio_encode", "pcm_s16le");
        params.put("lang", "autodialect");
        params.put("samplerate", String.valueOf(sampleRate));
        params.put("utc", UTC_FMT.format(ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))));
        params.put("uuid", sessionId);

        String base = params.entrySet().stream()
                .map(e -> urlEncode(e.getKey()) + "=" + urlEncode(e.getValue()))
                .reduce((a, b) -> a + "&" + b).orElse("");
        params.put("signature", hmacSha1(apiSecret, base));
        return endpoint + "?" + params.entrySet().stream()
                .map(e -> urlEncode(e.getKey()) + "=" + urlEncode(e.getValue()))
                .reduce((a, b) -> a + "&" + b).orElse("");
    }

    private String urlEncode(String v) { return URLEncoder.encode(v, StandardCharsets.UTF_8); }

    private String hmacSha1(String secret, String text) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(mac.doFinal(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("HmacSHA1 签名失败", e);
        }
    }

    // ===== WebSocket 监听器 =====

    private static class SpeechListener implements WebSocket.Listener {
        final CountDownLatch done = new CountDownLatch(1);
        final StringBuilder text = new StringBuilder();
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final StringBuilder buf = new StringBuilder();

        @Override public void onOpen(WebSocket webSocket) { webSocket.request(1); }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buf.append(data);
            if (last) {
                try {
                    JSONObject root = JSON.parseObject(buf.toString());
                    int code = root.getIntValue("code", 0);
                    if (code != 0) {
                        error.set(new Exception("讯飞错误 code=" + code + " " + root.getString("desc")));
                        done.countDown();
                    } else {
                        extractText(root);
                        if (isFinal(root)) done.countDown();
                    }
                } catch (Exception e) {
                    error.set(e);
                    done.countDown();
                }
                buf.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        @Override public CompletionStage<?> onClose(WebSocket ws, int code, String reason) { done.countDown(); return null; }
        @Override public void onError(WebSocket ws, Throwable err) { error.set(err); done.countDown(); }

        boolean await(long timeout, TimeUnit unit) throws InterruptedException { return done.await(timeout, unit); }

        private void extractText(JSONObject root) {
            JSONObject data = root.getJSONObject("data");
            if (data == null) return;
            JSONObject cn = data.getJSONObject("cn");
            if (cn == null) return;
            JSONObject st = cn.getJSONObject("st");
            if (st == null) return;
            var rt = st.getJSONArray("rt");
            if (rt == null) return;
            for (int i = 0; i < rt.size(); i++) {
                var ws = rt.getJSONObject(i).getJSONArray("ws");
                if (ws == null) continue;
                for (int j = 0; j < ws.size(); j++) {
                    var cw = ws.getJSONObject(j).getJSONArray("cw");
                    if (cw == null || cw.isEmpty()) continue;
                    String w = cw.getJSONObject(0).getString("w");
                    if (w != null) text.append(w);
                }
            }
        }

        private boolean isFinal(JSONObject root) {
            JSONObject data = root.getJSONObject("data");
            if (data == null) return false;
            Boolean ls = data.getBoolean("ls");
            return ls != null && ls;
        }
    }
}
