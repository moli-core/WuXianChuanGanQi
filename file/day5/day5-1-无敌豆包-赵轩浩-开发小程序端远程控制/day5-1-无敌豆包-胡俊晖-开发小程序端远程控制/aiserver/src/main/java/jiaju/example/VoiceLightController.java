package jiaju.example;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class VoiceLightController {
    private final XunFeiAsrService xunFeiAsrService;
    private final AiLightService aiLightService;
    private final MqttPublishUtil mqttPublishUtil;

    /**
     * 接收小程序录制音频
     * @param audio
     * @return
     */
    @PostMapping("/api/voice/light")
    public R voiceControl1(@RequestParam("audio") MultipartFile audio) {
        try {
            // 科大讯飞识别
            String voiceText = xunFeiAsrService.audioToText(audio);


            if (voiceText == null || voiceText.isBlank()) {
                return R.fail("语音识别失败，请重新说话");
            }
            log.info("识别语音文本：{}", voiceText);
            //大模型转换指令
            String cmd = aiLightService.parseStandardCmd(voiceText);
            log.info("AI解析指令：{}", cmd);
            //控制硬件设备
            mqttPublishUtil.sendLightCmd(cmd);
            return R.success()
                    .put("voiceText", voiceText)
                    .put("controlCmd", cmd);
        } catch (Exception e) {
            log.error("语音控制异常", e);
            return R.fail("服务异常：" + e.getMessage());
        }
    }

    /**
     * 测试接口 - 专门测试 MP3
     */
    @PostMapping("/voice/test/mp3")
    public R testMp3(@RequestParam("audio") MultipartFile audio) {
        try {
            String result = xunFeiAsrService.audioToTextMP3(audio);
            return R.success().put("result", result);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    //大模型转换
    @PostMapping("/api/voice/light1")
    public R voiceControl(@RequestParam("audio") MultipartFile audio) {
        try {
            // 推荐：使用直接转录方式（更简单可靠）
            String voiceText = aiLightService.audioToTextDirect(audio);

            // 或者使用异步方式（支持大文件）
            // String voiceText = aiLightService.audioToText(audio);

            if (voiceText.isBlank()) {
                return R.fail("语音识别失败，请重新说话");
            }
            log.info("识别语音文本：{}", voiceText);
            String cmd = aiLightService.parseStandardCmd(voiceText);
            log.info("AI解析指令：{}", cmd);
            mqttPublishUtil.sendLightCmd(cmd);
            return R.success()
                    .put("voiceText", voiceText)
                    .put("controlCmd", cmd);
        } catch (Exception e) {
            log.error("语音控制异常", e);
            return R.fail("服务异常：" + e.getMessage());
        }
    }

    /**
     * 手动输入文本，调用大模型解析指令控制灯光
     */
//    @PostMapping("/api/text/light")
//    public R textControl(@RequestParam String text) {
//        try {
//            if (text.isBlank()) {
//                return R.fail("输入文本不能为空");
//            }
//            log.info("手动输入文本：{}", text);
//
//            // AI解析标准控制指令
//            String cmd = aiLightService.parseStandardCmd(text);
//            log.info("AI解析指令：{}", cmd);
//
//            // MQTT下发ESP32硬件
//            mqttPublishUtil.sendLightCmd(cmd);
//
//            return R.success()
//                    .put("inputText", text)
//                    .put("controlCmd", cmd);
//        } catch (Exception e) {
//            log.error("文本控制灯光异常", e);
//            return R.fail("服务异常：" + e.getMessage());
//        }
//    }

    /**
     * 小程序前端 发送 开灯指令
     * @param request
     * @return
     */
    @PostMapping("/api/text/light")
    public R textControl(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            if (text == null || text.isBlank()) {
                return R.fail("输入文本不能为空");
            }
            log.info("手动输入文本：{}", text);

            // AI解析标准控制指令
            String cmd = aiLightService.parseStandardCmd(text);
            log.info("AI解析指令：{}", cmd);

            // MQTT下发ESP32硬件
            mqttPublishUtil.sendLightCmd(cmd);

            return R.success()
                    .put("inputText", text)
                    .put("controlCmd", cmd);
        } catch (Exception e) {
            log.error("文本控制灯光异常", e);
            return R.fail("服务异常：" + e.getMessage());
        }
    }

    /**
     * 手动输入文本，调用大模型解析指令控制灯光（GET接口，浏览器地址栏可直接测试）
     */
    @GetMapping("/api/text/light")
    public R textControl1(@RequestParam String text) {
        try {
            if (text.isBlank()) {
                return R.fail("输入文本不能为空");
            }
            log.info("手动输入文本：{}", text);

            // AI解析标准控制指令
            String cmd = aiLightService.parseStandardCmd(text);
            log.info("AI解析指令：{}", cmd);

            // MQTT下发ESP32硬件
            mqttPublishUtil.sendLightCmd(cmd);

            return R.success()
                    .put("inputText", text)
                    .put("controlCmd", cmd);
        } catch (Exception e) {
            log.error("文本控制灯光异常", e);
            return R.fail("服务异常：" + e.getMessage());
        }
    }




}