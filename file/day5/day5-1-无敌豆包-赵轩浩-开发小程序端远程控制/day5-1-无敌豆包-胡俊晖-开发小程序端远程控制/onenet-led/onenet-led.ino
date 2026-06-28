#include <WiFi.h>
#include <PubSubClient.h>

#define MAX_RETRIES 20

// WiFi配置
const char *ssid     = "ryX50";
const char *password = "zy123456";

// 巴法云参数
const char *mqtt_broker  = "mqtt.bemfa.com";
const int mqtt_port      = 9501;
const char *client_id    = "30bb2e98359643078a1f1f35f3e579d5";
const char *mqtt_uid     = "30bb2e98359643078a1f1f35f3e579d5";
const char *mqtt_pwd     = "bemfa";

// 注意：小程序里主题是light002，你这里写的led002，二者必须严格一致！
const char *topic_sub    = "led002";

WiFiClient espClient;
PubSubClient client(espClient);

// 消息回调
void callback(char *topic, byte *payload, unsigned int length)
{
  Serial.print("收到主题：");
  Serial.println(topic);
  String msg;
  for(int i=0;i<length;i++) msg += (char)payload[i];
  Serial.print("下发指令：");
  Serial.println(msg);

  // 改动：on亮灯，off灭灯
  if(msg == "on"){
    digitalWrite(2, HIGH);
    Serial.println("LED点亮");
  }else if(msg == "off"){
    digitalWrite(2, LOW);
    Serial.println("LED熄灭");
  }
}

void connectWiFi()
{
  Serial.print("连接WiFi：");
  Serial.println(ssid);
  WiFi.begin(ssid, password);
  int cnt = 0;
  while(WiFi.status() != WL_CONNECTED && cnt < MAX_RETRIES)
  {
    delay(1000);
    Serial.print(".");
    cnt++;
  }
  if(WiFi.status() == WL_CONNECTED)
  {
    Serial.println("\nWiFi连接成功，IP：" + WiFi.localIP().toString());
  }
}

void mqttReconnect()
{
  while(!client.connected())
  {
    Serial.println("正在连接巴法云MQTT...");
    bool connRes = client.connect(client_id, mqtt_uid, mqtt_pwd);
    if(connRes)
    {
      Serial.println("✅ MQTT连接成功！");
      client.subscribe(topic_sub);
      Serial.print("已订阅主题：");
      Serial.println(topic_sub);
      client.setCallback(callback);
    }
    else
    {
      Serial.print("❌ 连接失败，错误码：");
      Serial.println(client.state());
      delay(2000);
    }
  }
}

void setup()
{
  Serial.begin(115200);
  pinMode(2, OUTPUT);
  digitalWrite(2, LOW);
  connectWiFi();
  client.setServer(mqtt_broker, mqtt_port);
}

void loop()
{
  if(WiFi.status() != WL_CONNECTED) connectWiFi();
  if(!client.connected()) mqttReconnect();
  client.loop();
  delay(500);
}