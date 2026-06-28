package jiaju.example;

import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioTranscriptionModel;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioTranscriptionOptions;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiLightService {
    private final DashScopeAudioTranscriptionModel audioTranscriptionModel;
    private final DashScopeChatModel chatModel;

    @Value("${spring.ai.dashscope.api-key}")
    private String DASHSCOPE_API_KEY;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final Logger log = LoggerFactory.getLogger(AiLightService.class);

    private static final String FILE_UPLOAD_URL = "https://dashscope.aliyuncs.com/api/v1/files";
    private static final String ASR_TRANS_URL = "https://dashscope.aliyuncs.com/api/v1/services/audio/asr/transcription";
    private static final String TASK_QUERY_URL = "https://dashscope.aliyuncs.com/api/v1/tasks";

    /**
     * 音频转文字 - 完整流程（异步方式）
     */
    public String audioToText(MultipartFile audio) throws Exception {
        File tempLocalAudio = File.createTempFile("voice_temp_", ".mp3");
        try {
            audio.transferTo(tempLocalAudio);
            log.info("临时文件创建成功：{}，大小：{} bytes", tempLocalAudio.getAbsolutePath(), tempLocalAudio.length());

            // ========== 1. 上传文件到百炼 ==========
            String fileUrl = uploadFile(tempLocalAudio);
            log.info("文件上传成功，file_url：{}", fileUrl);

            // ========== 2. 异步语音识别 ==========
            String voiceText = transcribeAudioAsync(fileUrl);
            log.info("语音识别结果：{}", voiceText);
            return voiceText;

        } finally {
            if (tempLocalAudio.exists() && tempLocalAudio.delete()) {
                log.info("临时文件已清理");
            }
        }
    }

    /**
     * 上传文件到阿里云百炼
     */
    private String uploadFile(File file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + DASHSCOPE_API_KEY);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("files", new FileSystemResource(file));
            body.add("purpose", "file-extract");

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            log.info("开始上传文件到百炼，URL：{}", FILE_UPLOAD_URL);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    FILE_UPLOAD_URL,
                    requestEntity,
                    String.class
            );

            log.info("上传响应：{}", response.getBody());

            JSONObject jsonResponse = JSONObject.parseObject(response.getBody());

            if (jsonResponse.containsKey("code") && !"OK".equals(jsonResponse.getString("code"))) {
                throw new RuntimeException("上传失败：" + jsonResponse.getString("message"));
            }

            JSONObject data = jsonResponse.getJSONObject("data");
            if (data != null) {
                JSONArray uploadedFiles = data.getJSONArray("uploaded_files");
                if (uploadedFiles != null && !uploadedFiles.isEmpty()) {
                    JSONObject fileInfo = uploadedFiles.getJSONObject(0);
                    String fileUrl = fileInfo.getString("url");
                    if (fileUrl != null && !fileUrl.isEmpty()) {
                        return fileUrl;
                    }
                }
            }

            throw new RuntimeException("无法从响应中提取file_url：" + response.getBody());

        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败：" + e.getMessage(), e);
        }
    }

    /**
     * 异步语音识别 - 提交任务并轮询结果
     */
    private String transcribeAudioAsync(String fileUrl) throws InterruptedException {
        // 1. 提交异步任务
        String taskId = submitTranscriptionTask(fileUrl);
        log.info("语音识别任务已提交，task_id：{}", taskId);

        // 2. 轮询查询任务结果
        return pollTranscriptionResult(taskId);
    }

    /**
     * 提交异步语音识别任务
     */
    private String submitTranscriptionTask(String fileUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + DASHSCOPE_API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 关键：添加异步标识头
        headers.set("X-DashScope-Async", "enable");

        // 构建请求体
        Map<String, Object> input = new HashMap<>();
        // 重要：file_urls 必须是字符串数组，不能是单个字符串！
        input.put("file_urls", new String[]{fileUrl});

        Map<String, Object> body = new HashMap<>();
        body.put("model", "paraformer-v2");
        body.put("input", input);

        // 可选参数
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("enable_itn", true);  // 启用逆文本正则化
        parameters.put("enable_voiceprint", false);
        body.put("parameters", parameters);

        log.info("提交语音识别任务，file_url：{}", fileUrl);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                ASR_TRANS_URL,
                requestEntity,
                String.class
        );

        log.info("提交任务响应：{}", response.getBody());

        JSONObject jsonResponse = JSONObject.parseObject(response.getBody());

        // 检查是否有错误
        if (jsonResponse.containsKey("code") && !"OK".equals(jsonResponse.getString("code"))) {
            throw new RuntimeException("提交任务失败：" + jsonResponse.getString("message"));
        }

        // 提取task_id
        JSONObject output = jsonResponse.getJSONObject("output");
        if (output != null) {
            String taskId = output.getString("task_id");
            if (taskId != null && !taskId.isEmpty()) {
                return taskId;
            }
        }

        throw new RuntimeException("无法从响应中提取task_id：" + response.getBody());
    }

    /**
     * 轮询查询语音识别任务结果
     */
    private String pollTranscriptionResult(String taskId) throws InterruptedException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + DASHSCOPE_API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);

        int maxRetries = 60; // 最多轮询60次
        int retryCount = 0;
        long waitTime = 2000; // 每次等待2秒

        while (retryCount < maxRetries) {
            String queryUrl = TASK_QUERY_URL + "/" + taskId;

            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    queryUrl,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            JSONObject jsonResponse = JSONObject.parseObject(response.getBody());

            // 检查是否有错误
            if (jsonResponse.containsKey("code") && !"OK".equals(jsonResponse.getString("code"))) {
                throw new RuntimeException("查询任务状态失败：" + jsonResponse.getString("message"));
            }

            JSONObject output = jsonResponse.getJSONObject("output");
            if (output == null) {
                throw new RuntimeException("响应中缺少output字段：" + response.getBody());
            }

            String taskStatus = output.getString("task_status");
            log.info("任务状态：{}，重试次数：{}", taskStatus, retryCount);

            if ("SUCCEEDED".equals(taskStatus)) {
                // 任务成功，解析结果
                return parseTranscriptionResult(output);
            } else if ("FAILED".equals(taskStatus)) {
                String errorMsg = output.getString("message");
                throw new RuntimeException("语音识别任务失败：" + errorMsg);
            } else if ("PENDING".equals(taskStatus) || "RUNNING".equals(taskStatus)) {
                // 任务进行中，等待后继续轮询
                Thread.sleep(waitTime);
                retryCount++;
            } else {
                throw new RuntimeException("未知的任务状态：" + taskStatus);
            }
        }

        throw new RuntimeException("语音识别任务超时，task_id：" + taskId);
    }

    /**
     * 解析语音识别结果
     */
    private String parseTranscriptionResult(JSONObject output) {
        // 尝试多种可能的结果结构

        // 方式1：直接获取text字段
        String text = output.getString("text");
        if (text != null && !text.isEmpty()) {
            return text;
        }

        // 方式2：从results数组中获取
        JSONArray results = output.getJSONArray("results");
        if (results != null && !results.isEmpty()) {
            StringBuilder fullText = new StringBuilder();
            for (int i = 0; i < results.size(); i++) {
                JSONObject result = results.getJSONObject(i);
                String resultText = result.getString("text");
                if (resultText != null) {
                    fullText.append(resultText);
                }
            }
            if (fullText.length() > 0) {
                return fullText.toString();
            }
        }

        // 方式3：从sentences中获取
        JSONArray sentences = output.getJSONArray("sentences");
        if (sentences != null && !sentences.isEmpty()) {
            StringBuilder fullText = new StringBuilder();
            for (int i = 0; i < sentences.size(); i++) {
                JSONObject sentence = sentences.getJSONObject(i);
                String sentenceText = sentence.getString("text");
                if (sentenceText != null) {
                    fullText.append(sentenceText);
                }
            }
            if (fullText.length() > 0) {
                return fullText.toString();
            }
        }

        // 方式4：从transcripts中获取
        JSONArray transcripts = output.getJSONArray("transcripts");
        if (transcripts != null && !transcripts.isEmpty()) {
            JSONObject transcript = transcripts.getJSONObject(0);
            String transcriptText = transcript.getString("text");
            if (transcriptText != null && !transcriptText.isEmpty()) {
                return transcriptText;
            }
        }

        throw new RuntimeException("无法解析识别结果：" + output.toJSONString());
    }

    /**
     * 简化方案：直接使用Spring AI的音频转录（同步方式，推荐用于小文件）
     * 这个方法不需要文件上传，更简单可靠
     */
    public String audioToTextDirect(MultipartFile audio) throws Exception {
        File tempFile = File.createTempFile("voice_", ".mp3");
        try {
            audio.transferTo(tempFile);
            FileSystemResource audioResource = new FileSystemResource(tempFile);

            DashScopeAudioTranscriptionOptions options = DashScopeAudioTranscriptionOptions.builder()
                    .model("paraformer-realtime-v2")  // 使用实时模型，支持同步调用
                    .build();

            AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioResource, options);
            AudioTranscriptionResponse response = audioTranscriptionModel.call(prompt);

            return response.getResult().getOutput();
        } finally {
            if (tempFile.exists() && tempFile.delete()) {
                log.info("临时文件已清理");
            }
        }
    }




    /**
     * AI解析语音文本输出标准指令
     */
    public String parseStandardCmd(String voiceText) {
        String prompt = """
                你是智能家居指令转换器，仅输出规定内容，禁止多余文字、解释、符号。
                规则：
                1. 开灯、打开灯光、亮灯 → on
                2. 关灯、熄灭灯光、关掉灯 → off
                3. 亮度三十、调到70、调亮一点、暗一点 → 输出0-100之间数字
                用户语音文本：%s
                """.formatted(voiceText);

        Prompt promptObj = new Prompt(new UserMessage(prompt));
        String result = chatModel.call(promptObj).getResult().getOutput().getText();
        return result.trim();
    }
}