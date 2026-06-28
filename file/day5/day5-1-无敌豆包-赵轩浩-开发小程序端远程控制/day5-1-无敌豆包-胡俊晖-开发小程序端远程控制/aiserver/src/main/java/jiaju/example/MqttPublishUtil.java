package jiaju.example;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MqttPublishUtil {

    @Value("${mqtt.broker}")
    private String broker;

    @Value("${mqtt.uid}")
    private String uid;

    @Value("${mqtt.pwd}")
    private String pwd;

    @Value("${mqtt.topic}")
    private String topic;

    private MqttClient mqttClient;

    /**
     * 项目启动自动初始化MQTT连接
     */
    @PostConstruct
    public void init() {
        try {
            // 清除参数前后隐形空格
            String realUid = uid.trim();
            String realPwd = pwd.trim();
            // 随机ClientId，防止和ESP32固定ClientId冲突互踢
            String clientId = "9447dfa42cea4115bb221dc9f5db4ba4";

            mqttClient = new MqttClient(broker, clientId, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();

            // 巴法云9501明文端口标准鉴权（和ESP32硬件完全一致）
            options.setUserName(realUid);
            options.setPassword(realPwd.toCharArray());

            options.setCleanSession(true);
            options.setConnectionTimeout(2); // 连接2秒超时，避免卡死
            options.setKeepAliveInterval(30);
            options.setAutomaticReconnect(true);

            log.info("MQTT初始化鉴权参数 username:{} , password:{}", realUid, realPwd);
            mqttClient.connect(options);
            log.info("✅ 巴法云MQTT连接初始化成功");

        } catch (MqttSecurityException e) {
            log.error("❌ MQTT鉴权失败【无权连接】，核对uid与pwd配置", e);
        } catch (MqttException e) {
            log.error("❌ MQTT初始化连接失败，检查broker/端口/网络", e);
        } catch (Exception e) {
            log.error("❌ MQTT初始化未知异常", e);
        }
    }

    /**
     * 下发灯光控制指令
     * 异步发送，不阻塞主线程，调用完立即返回Controller执行return
     * @param cmd 控制指令 on / off
     */
    public void sendLightCmd(String cmd) {
        if (mqttClient == null) {
            throw new RuntimeException("MQTT客户端未初始化，连接失败");
        }

        // 断线重连逻辑（带2秒超时，不会卡死）
        if (!mqttClient.isConnected()) {
            log.warn("MQTT当前离线，执行重连逻辑");
            try {
                String realUid = uid.trim();
                String realPwd = pwd.trim();
                MqttConnectOptions options = new MqttConnectOptions();
                options.setUserName(realUid);
                options.setPassword(realPwd.toCharArray());
                options.setCleanSession(true);
                options.setConnectionTimeout(2);
                options.setKeepAliveInterval(30);
                options.setAutomaticReconnect(true);
                mqttClient.connect(options);
                log.info("✅ MQTT离线重连成功");
            } catch (MqttException e) {
                log.error("❌ MQTT重连失败，丢弃指令:{}", cmd, e);
                throw new RuntimeException("MQTT离线重连失败，指令下发中断", e);
            }
        }

        // 新开子线程执行阻塞publish，不占用接口主线程
        new Thread(() -> {
            try {
                final String fullTopic =  topic;
                final MqttMessage message = new MqttMessage(cmd.getBytes());
                message.setQos(1);
                // 原生双参数方法，无编译报错
                mqttClient.publish(fullTopic, message);
                log.info("✅ MQTT消息发送完成，主题={}，指令={}", fullTopic, cmd);
            } catch (MqttException e) {
                log.error("❌ MQTT子线程发送异常，指令:{}", cmd, e);
            }
        }).start();

        // 主线程直接走到这里，不会阻塞，接口正常返回
        log.info("MQTT发送任务已提交子线程，接口放行返回");
    }

    /**
     * 项目销毁时关闭MQTT连接
     */
    @PreDestroy
    public void close() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                log.info("MQTT客户端正常断开连接");
            }
        } catch (MqttException e) {
            log.error("❌ 关闭MQTT连接时异常", e);
        }
    }
}