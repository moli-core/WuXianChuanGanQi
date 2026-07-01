#include <Arduino.h>
#include <WiFi.h>
#include <Wire.h>
#include <PubSubClient.h>
#include <driver/i2s.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <DHT.h>

#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
#define OLED_RESET_PIN -1
#define OLED_I2C_ADDRESS 0x3C
#define OLED_SDA_PIN 21
#define OLED_SCL_PIN 22

// Green LED keeps the button + cloud control it always had. Red and yellow are
// cloud-only (no button) and reported as Status_ledRed / Status_ledyellow.
#define LED_PIN 17        // green
#define LED_RED_PIN 18    // cloud-only
#define LED_YELLOW_PIN 19 // cloud-only
#define BUTTON_PIN 16
#define VOICE_BUTTON_PIN 4
#define PIR_PIN 13
#define BUZZER_PIN 25
#define BUZZER_BUTTON_PIN 26
// Most MH-FMD active-buzzer modules are active-low (I/O low = sound). Flip this to
// HIGH if your module sounds when idle.
#define BUZZER_ACTIVE_LEVEL LOW

#define DHT_PIN 27
#define DHT_TYPE DHT11

#define I2S_MIC_BCLK_PIN 14
#define I2S_MIC_WS_PIN 15
#define I2S_MIC_DATA_PIN 32
#define I2S_MIC_PORT I2S_NUM_0

Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET_PIN);
DHT dht(DHT_PIN, DHT_TYPE);

// Fill in your phone 2.4G hotspot name and password.
const char *wifi_ssid = "baohan";
const char *wifi_pwd = "88888887";

const char *mqtt_server = "d1aaeb8e71.st1.iotda-device.cn-south-1.myhuaweicloud.com";
const uint16_t mqtt_port = 1883;
const char *mqtt_client_id = "6a3e4b21c00ccb6d4b6000c5_Esp32_Device_0_0_2026062610";
const char *mqtt_username = "6a3e4b21c00ccb6d4b6000c5_Esp32_Device";
const char *mqtt_password = "e1afac7e15ca83eca6d5d5da214088a8133e4fa8c86e7c1caec0ee72d46cb048";

const char *topic_down = "$oc/devices/6a3e4b21c00ccb6d4b6000c5_Esp32_Device/sys/messages/down";
const char *topic_property_set = "$oc/devices/6a3e4b21c00ccb6d4b6000c5_Esp32_Device/sys/properties/set/#";
const char *topic_report = "$oc/devices/6a3e4b21c00ccb6d4b6000c5_Esp32_Device/sys/properties/report";

const char *voice_backend_host = "192.168.198.114";
const uint16_t voice_backend_port = 8080;
const char *voice_backend_path = "/api/voice/light";

WiFiClient wifi_client;
PubSubClient mqtt_client(wifi_client);

int last_button_state = HIGH;
bool button_pressed = false;
int last_voice_button_state = HIGH;
bool led_status = false;
bool led_red_status = false;
bool led_yellow_status = false;
bool need_report = false;
int dht_temp = 0;
int dht_humi = 0;
bool dht_valid = false;
int pir_state = 0;
bool buzzer_on = false;
bool buzzer_manual = false;
int last_buzzer_button_state = HIGH;
unsigned long last_mqtt_reconnect_ms = 0;
unsigned long last_status_report_ms = 0;
unsigned long last_dht_read_ms = 0;
const unsigned long MQTT_RECONNECT_INTERVAL_MS = 5000;
const unsigned long STATUS_REPORT_INTERVAL_MS = 3000;
// DHT11 cannot be sampled faster than about once per second, so read on an interval.
const unsigned long DHT_READ_INTERVAL_MS = 2000;
const unsigned long BUTTON_DEBOUNCE_MS = 20;
// Environment alarm thresholds: buzz when temperature or humidity exceeds these.
const int BUZZER_TEMP_THRESHOLD = 30;
const int BUZZER_HUMI_THRESHOLD = 95;
const int VOICE_SAMPLE_RATE = 16000;
// Recording now runs until the user presses the button a second time, so the
// length is not fixed. We keep a safety cap so a forgotten stop can't record
// forever (which would also waste backend memory and ASR billing).
const int VOICE_MAX_SECONDS = 5;
const size_t VOICE_MAX_PCM_BYTES = (size_t)VOICE_SAMPLE_RATE * VOICE_MAX_SECONDS * sizeof(int16_t);
// INMP441 outputs 24-bit data left-aligned inside a 32-bit I2S frame, so we must
// sample at 32 bits and down-convert to the 16-bit PCM the mini program expects.
// Measured peak raw sample is ~2.1e9 (near full 32-bit scale), so we shift right
// by 17 to land loud speech around half of the 16-bit range without clipping.
// Lower this value if audio is too quiet; raise it if it clips/saturates.
const int VOICE_SAMPLE_SHIFT = 17;
// How long to wait for the voice backend's HTTP response after the upload ends.
// Kept short so a slow/unresponsive backend can't stall the main loop (and with
// it MQTT/buttons/sensors) for long.
const unsigned long VOICE_RESPONSE_TIMEOUT_MS = 5000;

const char *mqttStateText(int state) {
  switch (state) {
    case MQTT_CONNECTION_TIMEOUT:
      return "connection timeout";
    case MQTT_CONNECTION_LOST:
      return "connection lost";
    case MQTT_CONNECT_FAILED:
      return "tcp connect failed";
    case MQTT_DISCONNECTED:
      return "disconnected";
    case MQTT_CONNECTED:
      return "connected";
    case MQTT_CONNECT_BAD_PROTOCOL:
      return "bad protocol";
    case MQTT_CONNECT_BAD_CLIENT_ID:
      return "bad client id";
    case MQTT_CONNECT_UNAVAILABLE:
      return "server unavailable";
    case MQTT_CONNECT_BAD_CREDENTIALS:
      return "bad username or password";
    case MQTT_CONNECT_UNAUTHORIZED:
      return "unauthorized";
    default:
      return "unknown";
  }
}

void drawStatus(const char *message) {
  display.clearDisplay();
  display.setTextSize(1);
  display.setTextColor(SSD1306_WHITE);
  display.setCursor(0, 0);

  display.println("ESP32 Huawei IoT");
  display.print("WiFi:");
  display.println(WiFi.status() == WL_CONNECTED ? "Connected" : "Disconnected");

  if (WiFi.status() == WL_CONNECTED) {
    display.print("IP:");
    display.println(WiFi.localIP());
    display.print("Signal:");
    display.print(WiFi.RSSI());
    display.println(" dBm");
  }

  display.print("MQTT:");
  display.println(mqtt_client.connected() ? "Connected" : "Disconnected");
  display.print("LED:");
  display.println(led_status ? "ON" : "OFF");
  display.print("T:");
  if (dht_valid) {
    display.print(dht_temp);
    display.print("C H:");
    display.print(dht_humi);
  } else {
    display.print("-- H:--");
  }
  display.print(" P:");
  display.println(pir_state ? "Y" : "N");
  display.println(message);
  display.display();
}

void setLed(bool on) {
  led_status = on;
  digitalWrite(LED_PIN, led_status ? LOW : HIGH);
}

// Red/yellow LEDs are cloud-only and active-low like the green one.
void setLedRed(bool on) {
  led_red_status = on;
  digitalWrite(LED_RED_PIN, led_red_status ? LOW : HIGH);
}

void setLedYellow(bool on) {
  led_yellow_status = on;
  digitalWrite(LED_YELLOW_PIN, led_yellow_status ? LOW : HIGH);
}

// Drives the buzzer pin honoring the module's active level.
void setBuzzer(bool on) {
  buzzer_on = on;
  digitalWrite(BUZZER_PIN, on ? BUZZER_ACTIVE_LEVEL : !BUZZER_ACTIVE_LEVEL);
}

// The buzzer sounds when manually enabled by its button OR when the environment
// exceeds a threshold. Call this whenever a relevant input changes.
void updateBuzzer() {
  bool env_alarm = dht_valid &&
                   (dht_temp > BUZZER_TEMP_THRESHOLD || dht_humi > BUZZER_HUMI_THRESHOLD);
  bool should_sound = buzzer_manual || env_alarm;
  if (should_sound != buzzer_on) {
    setBuzzer(should_sound);
  }
}

// Reads the DHT11 on its interval. DHT11 resolution is 1 unit, so we round to int
// to match the cloud Data_temp/Data_humi integer properties. NaN readings (timing
// glitches) are ignored, keeping the previous valid values on screen.
void updateDht() {
  if (millis() - last_dht_read_ms < DHT_READ_INTERVAL_MS) {
    return;
  }
  last_dht_read_ms = millis();

  float humidity = dht.readHumidity();
  float temperature = dht.readTemperature();
  if (isnan(humidity) || isnan(temperature)) {
    Serial.println("DHT read failed (NaN), keeping last values.");
    return;
  }

  int new_temp = (int)lroundf(temperature);
  int new_humi = (int)lroundf(humidity);
  bool changed = !dht_valid || new_temp != dht_temp || new_humi != dht_humi;
  dht_temp = new_temp;
  dht_humi = new_humi;
  dht_valid = true;

  // Refresh the OLED when a value changes so the display stays current even when
  // no button/report event is redrawing the screen.
  if (changed) {
    drawStatus("Sensor Updated");
  }
}

bool reportLedStatus(bool visible);

// Reads the HC-SR501 PIR output (HIGH = motion detected). On a state change we
// refresh the OLED and push an immediate report so presence reaches the cloud
// promptly instead of waiting for the next periodic report.
void updatePir() {
  int level = digitalRead(PIR_PIN);

  int new_state = level == HIGH ? 1 : 0;
  if (new_state == pir_state) {
    return;
  }

  pir_state = new_state;
  Serial.print("PIR state changed: ");
  Serial.println(pir_state ? "MOTION" : "clear");
  drawStatus(pir_state ? "Motion Detected" : "No Motion");
  reportLedStatus(true);
}

void initVoiceRecorder() {
  i2s_config_t i2s_config = {};
  i2s_config.mode = (i2s_mode_t)(I2S_MODE_MASTER | I2S_MODE_RX);
  i2s_config.sample_rate = VOICE_SAMPLE_RATE;
  i2s_config.bits_per_sample = I2S_BITS_PER_SAMPLE_32BIT;
  // INMP441 with L/R(SEL) tied to GND actually transmits on the I2S RIGHT slot on
  // ESP32 (confirmed by the reference example). Using ONLY_LEFT here reads the
  // empty slot and yields all-zero samples, so we must select ONLY_RIGHT.
  i2s_config.channel_format = I2S_CHANNEL_FMT_ONLY_RIGHT;
  i2s_config.communication_format = I2S_COMM_FORMAT_STAND_I2S;
  i2s_config.intr_alloc_flags = ESP_INTR_FLAG_LEVEL1;
  i2s_config.dma_buf_count = 4;
  i2s_config.dma_buf_len = 512;
  i2s_config.use_apll = false;
  i2s_config.tx_desc_auto_clear = false;
  i2s_config.fixed_mclk = 0;

  i2s_pin_config_t pin_config = {};
  pin_config.bck_io_num = I2S_MIC_BCLK_PIN;
  pin_config.ws_io_num = I2S_MIC_WS_PIN;
  pin_config.data_out_num = I2S_PIN_NO_CHANGE;
  pin_config.data_in_num = I2S_MIC_DATA_PIN;

  esp_err_t driver_result = i2s_driver_install(I2S_MIC_PORT, &i2s_config, 0, nullptr);
  esp_err_t pin_result = i2s_set_pin(I2S_MIC_PORT, &pin_config);
  i2s_zero_dma_buffer(I2S_MIC_PORT);

  Serial.print("I2S recorder init: driver=");
  Serial.print(driver_result);
  Serial.print(", pins=");
  Serial.println(pin_result);
}

// Detects a fresh press (HIGH->LOW edge) of the voice button, used to stop an
// in-progress recording. Keeps last_voice_button_state in sync so the press that
// stops recording is not re-read as a new "start" press afterwards.
bool voiceButtonPressedEdge() {
  int state = digitalRead(VOICE_BUTTON_PIN);
  if (state == LOW && last_voice_button_state == HIGH) {
    delay(BUTTON_DEBOUNCE_MS);
    if (digitalRead(VOICE_BUTTON_PIN) == LOW) {
      last_voice_button_state = LOW;
      return true;
    }
  } else if (state == HIGH && last_voice_button_state == LOW) {
    last_voice_button_state = HIGH;
  }
  return false;
}

// Writes one HTTP chunked-transfer chunk: hex length, CRLF, data, CRLF.
void writeHttpChunk(WiFiClient &client, const uint8_t *data, size_t length) {
  if (length == 0) {
    return;
  }
  client.printf("%X\r\n", (unsigned)length);
  client.write(data, length);
  client.print("\r\n");
}

// Records and uploads the voice clip in one streaming pass. Recording runs until
// the user taps the voice button again (or VOICE_MAX_SECONDS elapses), so the
// length is unknown up front. We therefore use HTTP chunked transfer encoding
// instead of a fixed Content-Length, writing each freshly-converted PCM block as
// its own chunk. This also keeps memory flat (no 128KB buffer to allocate).
bool streamRecordAndUpload() {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("Voice upload skipped: WiFi disconnected.");
    return false;
  }

  WiFiClient client;
  if (!client.connect(voice_backend_host, voice_backend_port)) {
    Serial.println("Voice backend connect failed.");
    return false;
  }

  const String boundary = "----wsnEsp32VoiceBoundary";
  String prefix;
  prefix += "--" + boundary + "\r\n";
  prefix += "Content-Disposition: form-data; name=\"sampleRate\"\r\n\r\n";
  prefix += String(VOICE_SAMPLE_RATE) + "\r\n";
  prefix += "--" + boundary + "\r\n";
  prefix += "Content-Disposition: form-data; name=\"channels\"\r\n\r\n1\r\n";
  prefix += "--" + boundary + "\r\n";
  prefix += "Content-Disposition: form-data; name=\"encoding\"\r\n\r\npcm\r\n";
  prefix += "--" + boundary + "\r\n";
  prefix += "Content-Disposition: form-data; name=\"audio\"; filename=\"esp32.pcm\"\r\n";
  prefix += "Content-Type: application/octet-stream\r\n\r\n";

  String suffix = "\r\n--" + boundary + "--\r\n";

  client.print("POST ");
  client.print(voice_backend_path);
  client.println(" HTTP/1.1");
  client.print("Host: ");
  client.print(voice_backend_host);
  client.print(":");
  client.println(voice_backend_port);
  client.print("Content-Type: multipart/form-data; boundary=");
  client.println(boundary);
  client.println("Transfer-Encoding: chunked");
  client.println("Connection: close");
  client.println();

  // Multipart prefix is the first chunk.
  writeHttpChunk(client, (const uint8_t *)prefix.c_str(), prefix.length());

  // Stream the PCM body: read 32-bit INMP441 samples, down-convert to 16-bit, send
  // each block as its own HTTP chunk. Recording continues until the user taps the
  // voice button again or the safety cap (VOICE_MAX_SECONDS) is reached.
  drawStatus("Voice Recording");
  Serial.println("Voice recording start. Press again to stop.");
  i2s_zero_dma_buffer(I2S_MIC_PORT);
  // The start press is still being held (state LOW). Keep last state LOW so the
  // stop is only detected after a real release-then-press, not the held start tap.
  last_voice_button_state = LOW;

  const size_t CHUNK_SAMPLES = 256;
  int32_t raw_samples[CHUNK_SAMPLES];
  int16_t pcm_chunk[CHUNK_SAMPLES];
  const size_t max_samples = VOICE_MAX_PCM_BYTES / sizeof(int16_t);
  size_t produced = 0;
  unsigned long start_record_ms = millis();
  // Diagnostics: track loudest raw 32-bit sample and loudest 16-bit output sample
  // so the serial log shows whether we captured silence, good audio, or clipping.
  int32_t peak_raw = 0;
  int16_t peak_pcm = 0;

  while (produced < max_samples) {
    if (voiceButtonPressedEdge()) {
      Serial.println("Voice stop requested by button.");
      break;
    }
    if (millis() - start_record_ms >= (unsigned long)VOICE_MAX_SECONDS * 1000UL) {
      Serial.println("Voice max duration reached.");
      break;
    }

    size_t want_samples = min(CHUNK_SAMPLES, max_samples - produced);
    size_t bytes_read = 0;
    esp_err_t result = i2s_read(I2S_MIC_PORT, raw_samples,
                                want_samples * sizeof(int32_t), &bytes_read, portMAX_DELAY);
    if (result != ESP_OK || bytes_read == 0) {
      Serial.print("I2S read failed: ");
      Serial.println(result);
      client.stop();
      return false;
    }

    size_t got_samples = bytes_read / sizeof(int32_t);
    for (size_t i = 0; i < got_samples; i++) {
      int32_t raw = raw_samples[i];
      int32_t raw_abs = raw < 0 ? -raw : raw;
      if (raw_abs > peak_raw) {
        peak_raw = raw_abs;
      }
      int32_t scaled = raw >> VOICE_SAMPLE_SHIFT;
      if (scaled > INT16_MAX) {
        scaled = INT16_MAX;
      } else if (scaled < INT16_MIN) {
        scaled = INT16_MIN;
      }
      int16_t pcm = (int16_t)scaled;
      int16_t pcm_abs = pcm < 0 ? -pcm : pcm;
      if (pcm_abs > peak_pcm) {
        peak_pcm = pcm_abs;
      }
      pcm_chunk[i] = pcm;
    }
    writeHttpChunk(client, (const uint8_t *)pcm_chunk, got_samples * sizeof(int16_t));
    produced += got_samples;
  }

  // Multipart suffix chunk, then the terminating zero-length chunk.
  writeHttpChunk(client, (const uint8_t *)suffix.c_str(), suffix.length());
  client.print("0\r\n\r\n");
  Serial.print("Voice recording end. PCM bytes: ");
  Serial.println((unsigned)(produced * sizeof(int16_t)));
  Serial.print("Audio peak raw32=");
  Serial.print(peak_raw);
  Serial.print(" peak16=");
  Serial.print(peak_pcm);
  Serial.print(" / 32767  (shift=");
  Serial.print(VOICE_SAMPLE_SHIFT);
  Serial.println(")");

  unsigned long start_ms = millis();
  String status_line;
  while (client.connected() && millis() - start_ms < VOICE_RESPONSE_TIMEOUT_MS) {
    if (client.available()) {
      status_line = client.readStringUntil('\n');
      break;
    }
    delay(10);
  }

  String response;
  while (client.connected() || client.available()) {
    while (client.available()) {
      response += (char)client.read();
    }
    if (millis() - start_ms > VOICE_RESPONSE_TIMEOUT_MS) {
      break;
    }
    delay(10);
  }
  client.stop();

  Serial.print("Voice backend status: ");
  Serial.println(status_line);
  Serial.println(response);
  return status_line.indexOf("200") >= 0;
}

void recordAndUploadVoice() {
  bool uploaded = streamRecordAndUpload();
  drawStatus(uploaded ? "Voice Uploaded" : "Voice Failed");
}

bool reportLedStatus(bool visible = false) {
  if (!mqtt_client.connected()) {
    need_report = true;
    return false;
  }

  // Report green LED (Status_LED), the cloud-only red/yellow LEDs, buzzer, PIR
  // presence, plus temp/humidity once a valid DHT reading exists. Status_body is
  // a cloud bool (true/false).
  char payload[320];
  if (dht_valid) {
    snprintf(payload, sizeof(payload),
             "{\"services\":[{\"service_id\":\"Esp32\",\"properties\":"
             "{\"Status_LED\":%s,\"Status_ledRed\":%s,\"Status_ledYellow\":%s,"
             "\"Status_beeper\":%s,\"Data_temp\":%d,\"Data_humi\":%d,\"Status_body\":%s}}]}",
             led_status ? "true" : "false",
             led_red_status ? "true" : "false", led_yellow_status ? "true" : "false",
             buzzer_on ? "true" : "false", dht_temp, dht_humi,
             pir_state ? "true" : "false");
  } else {
    snprintf(payload, sizeof(payload),
             "{\"services\":[{\"service_id\":\"Esp32\",\"properties\":"
             "{\"Status_LED\":%s,\"Status_ledRed\":%s,\"Status_ledYellow\":%s,"
             "\"Status_beeper\":%s,\"Status_body\":%s}}]}",
             led_status ? "true" : "false",
             led_red_status ? "true" : "false", led_yellow_status ? "true" : "false",
             buzzer_on ? "true" : "false",
             pir_state ? "true" : "false");
  }

  bool ok = mqtt_client.publish(topic_report, payload);
  need_report = !ok;

  if (visible || !ok) {
    Serial.print("Report Status: ");
    Serial.println(ok ? payload : "failed");
    drawStatus(ok ? "Report OK" : "Report FAIL");
  }

  if (ok) {
    last_status_report_ms = millis();
  }
  return ok;
}

bool parseLedCommand(const String &payload, bool &target_status) {
  // Green LED uses Status_LED. The old loose "led" fallback is gone on purpose —
  // it would now also match Status_ledRed / Status_ledYellow and toggle the green
  // LED by mistake.
  int key_index = payload.indexOf("Status_LED");
  if (key_index < 0) {
    return false;
  }

  int true_index = payload.indexOf("true", key_index);
  int false_index = payload.indexOf("false", key_index);
  int on_index = payload.indexOf("ON", key_index);
  int off_index = payload.indexOf("OFF", key_index);
  int one_index = payload.indexOf("1", key_index);
  int zero_index = payload.indexOf("0", key_index);

  int first_on = true_index >= 0 ? true_index : on_index;
  if (first_on < 0) {
    first_on = one_index;
  }

  int first_off = false_index >= 0 ? false_index : off_index;
  if (first_off < 0) {
    first_off = zero_index;
  }

  if (first_on < 0 && first_off < 0) {
    return false;
  }

  target_status = first_off < 0 || (first_on >= 0 && first_on < first_off);
  return true;
}

// Mirrors parseLedCommand for the buzzer's Status_beeper property. Bounds the
// value search to the slice right after the key so a Status_LED value elsewhere
// in the same payload can't be mistaken for the buzzer's value.
bool parseBuzzerCommand(const String &payload, bool &target_status) {
  int key_index = payload.indexOf("Status_beeper");
  if (key_index < 0) {
    key_index = payload.indexOf("beeper");
  }
  if (key_index < 0) {
    return false;
  }

  int true_index = payload.indexOf("true", key_index);
  int false_index = payload.indexOf("false", key_index);
  int on_index = payload.indexOf("ON", key_index);
  int off_index = payload.indexOf("OFF", key_index);
  int one_index = payload.indexOf("1", key_index);
  int zero_index = payload.indexOf("0", key_index);

  int first_on = true_index >= 0 ? true_index : on_index;
  if (first_on < 0) {
    first_on = one_index;
  }

  int first_off = false_index >= 0 ? false_index : off_index;
  if (first_off < 0) {
    first_off = zero_index;
  }

  if (first_on < 0 && first_off < 0) {
    return false;
  }

  target_status = first_off < 0 || (first_on >= 0 && first_on < first_off);
  return true;
}

// Generic on/off parser for a named boolean property. Same logic as
// parseLedCommand/parseBuzzerCommand but keyed by an arbitrary field, used for
// the cloud-only Status_ledRed / Status_ledYellow LEDs.
bool parseBoolCommand(const String &payload, const char *key, bool &target_status) {
  int key_index = payload.indexOf(key);
  if (key_index < 0) {
    return false;
  }

  int true_index = payload.indexOf("true", key_index);
  int false_index = payload.indexOf("false", key_index);
  int on_index = payload.indexOf("ON", key_index);
  int off_index = payload.indexOf("OFF", key_index);
  int one_index = payload.indexOf("1", key_index);
  int zero_index = payload.indexOf("0", key_index);

  int first_on = true_index >= 0 ? true_index : on_index;
  if (first_on < 0) {
    first_on = one_index;
  }

  int first_off = false_index >= 0 ? false_index : off_index;
  if (first_off < 0) {
    first_off = zero_index;
  }

  if (first_on < 0 && first_off < 0) {
    return false;
  }

  target_status = first_off < 0 || (first_on >= 0 && first_on < first_off);
  return true;
}

void replyPropertySet(const char *topic) {
  String topic_text(topic);
  int request_index = topic_text.indexOf("request_id=");
  if (request_index < 0 || !mqtt_client.connected()) {
    return;
  }

  String request_id = topic_text.substring(request_index + strlen("request_id="));
  String response_topic = "$oc/devices/6a3e4b21c00ccb6d4b6000c5_Esp32_Device/sys/properties/set/response/request_id=";
  response_topic += request_id;

  mqtt_client.publish(response_topic.c_str(), "{\"result_code\":0,\"result_desc\":\"success\"}");
}

void mqttCallback(char *topic, byte *payload, unsigned int length) {
  Serial.print("Down topic: ");
  Serial.println(topic);
  Serial.print("Down payload: ");
  String message;
  message.reserve(length + 1);
  for (unsigned int i = 0; i < length; i++) {
    char ch = (char)payload[i];
    Serial.print(ch);
    message += ch;
  }
  Serial.println();

  bool handled = false;

  bool led_target = false;
  if (parseLedCommand(message, led_target)) {
    setLed(led_target);
    drawStatus(led_target ? "Cloud LED ON" : "Cloud LED OFF");
    Serial.print("LED set by cloud: ");
    Serial.println(led_target ? "ON" : "OFF");
    handled = true;
  }

  // Cloud control of the buzzer drives the same manual override the button uses,
  // so the environment alarm in updateBuzzer() still works independently.
  bool buzzer_target = false;
  if (parseBuzzerCommand(message, buzzer_target)) {
    buzzer_manual = buzzer_target;
    updateBuzzer();
    drawStatus(buzzer_target ? "Cloud Buzzer ON" : "Cloud Buzzer OFF");
    Serial.print("Buzzer set by cloud: ");
    Serial.println(buzzer_target ? "ON" : "OFF");
    handled = true;
  }

  // Cloud-only red/yellow LEDs (no button).
  bool led_red_target = false;
  if (parseBoolCommand(message, "Status_ledRed", led_red_target)) {
    setLedRed(led_red_target);
    drawStatus(led_red_target ? "Cloud Red ON" : "Cloud Red OFF");
    Serial.print("Red LED set by cloud: ");
    Serial.println(led_red_target ? "ON" : "OFF");
    handled = true;
  }

  bool led_yellow_target = false;
  if (parseBoolCommand(message, "Status_ledYellow", led_yellow_target)) {
    setLedYellow(led_yellow_target);
    drawStatus(led_yellow_target ? "Cloud Yellow ON" : "Cloud Yellow OFF");
    Serial.print("Yellow LED set by cloud: ");
    Serial.println(led_yellow_target ? "ON" : "OFF");
    handled = true;
  }

  if (handled) {
    reportLedStatus(true);
    replyPropertySet(topic);
  } else {
    Serial.println("No known control field (Status_LED/Status_ledRed/Status_ledYellow/Status_beeper) in down payload.");
  }
}

void connectWiFi() {
  if (WiFi.status() == WL_CONNECTED) {
    return;
  }

  Serial.print("Connecting phone hotspot: ");
  Serial.println(wifi_ssid);
  drawStatus("Connect Phone Hotspot");

  WiFi.mode(WIFI_STA);
  WiFi.begin(wifi_ssid, wifi_pwd);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println();
  Serial.print("WiFi connected, IP: ");
  Serial.println(WiFi.localIP());
  drawStatus("WiFi Connected OK");
}

bool connectMqtt() {
  if (mqtt_client.connected()) {
    return true;
  }

  Serial.println("Connecting Huawei Cloud MQTT...");
  Serial.print("MQTT server: ");
  Serial.print(mqtt_server);
  Serial.print(":");
  Serial.println(mqtt_port);
  Serial.print("ClientId length: ");
  Serial.println(strlen(mqtt_client_id));
  Serial.print("Username length: ");
  Serial.println(strlen(mqtt_username));
  drawStatus("MQTT Connecting");

  WiFiClient tcp_test_client;
  if (!tcp_test_client.connect(mqtt_server, mqtt_port)) {
    Serial.println("TCP test failed. Check network, hotspot, DNS, or port 1883.");
    drawStatus("MQTT TCP FAIL");
    return false;
  }
  tcp_test_client.stop();
  Serial.println("TCP test OK.");

  bool ok = mqtt_client.connect(mqtt_client_id, mqtt_username, mqtt_password);
  if (!ok) {
    int state = mqtt_client.state();
    Serial.print("MQTT connect failed, state=");
    Serial.print(state);
    Serial.print(" (");
    Serial.print(mqttStateText(state));
    Serial.println(")");
    drawStatus("MQTT Connect FAIL");
    return false;
  }

  mqtt_client.subscribe(topic_down);
  mqtt_client.subscribe(topic_property_set);
  Serial.println("MQTT connected.");
  drawStatus("MQTT Connected OK");

  need_report = true;
  return true;
}

void setup() {
  Serial.begin(115200);
  delay(200);

  pinMode(LED_PIN, OUTPUT);
  pinMode(LED_RED_PIN, OUTPUT);
  pinMode(LED_YELLOW_PIN, OUTPUT);
  pinMode(BUTTON_PIN, INPUT_PULLUP);
  pinMode(VOICE_BUTTON_PIN, INPUT_PULLUP);
  pinMode(PIR_PIN, INPUT);
  pinMode(BUZZER_PIN, OUTPUT);
  pinMode(BUZZER_BUTTON_PIN, INPUT_PULLUP);
  setLed(false);
  setLedRed(false);
  setLedYellow(false);
  setBuzzer(false);
  dht.begin();

  Wire.begin(OLED_SDA_PIN, OLED_SCL_PIN);
  if (!display.begin(SSD1306_SWITCHCAPVCC, OLED_I2C_ADDRESS)) {
    Serial.println("OLED init failed, check wiring or I2C address.");
    while (true) {
      delay(1000);
    }
  }

  last_button_state = digitalRead(BUTTON_PIN);
  setLed(last_button_state == LOW);
  drawStatus("Booting");

  mqtt_client.setServer(mqtt_server, mqtt_port);
  mqtt_client.setCallback(mqttCallback);
  mqtt_client.setKeepAlive(60);
  mqtt_client.setSocketTimeout(15);
  mqtt_client.setBufferSize(512);
  initVoiceRecorder();

  connectWiFi();
  connectMqtt();
  reportLedStatus(true);
}

void handleButton() {
  int button_state = digitalRead(BUTTON_PIN);

  if (button_state == LOW && last_button_state == HIGH) {
    delay(BUTTON_DEBOUNCE_MS);
    if (digitalRead(BUTTON_PIN) == LOW) {
      button_pressed = true;
      last_button_state = LOW;
    }
    return;
  }

  if (button_state == HIGH && last_button_state == LOW) {
    delay(BUTTON_DEBOUNCE_MS);
    if (digitalRead(BUTTON_PIN) == HIGH) {
      if (button_pressed) {
        setLed(!led_status);
        drawStatus("LED Toggled");
        reportLedStatus(true);
      }
      button_pressed = false;
      last_button_state = HIGH;
    }
  }
}

// Dedicated voice button on VOICE_BUTTON_PIN: tap once (active-low) to start
// recording, tap again to stop and upload. We trigger on the press edge here;
// the stop press is detected inside streamRecordAndUpload via voiceButtonPressedEdge.
void handleVoiceButton() {
  int voice_state = digitalRead(VOICE_BUTTON_PIN);

  if (voice_state == LOW && last_voice_button_state == HIGH) {
    delay(BUTTON_DEBOUNCE_MS);
    if (digitalRead(VOICE_BUTTON_PIN) == LOW) {
      last_voice_button_state = LOW;
      recordAndUploadVoice();
    }
    return;
  }

  if (voice_state == HIGH && last_voice_button_state == LOW) {
    delay(BUTTON_DEBOUNCE_MS);
    if (digitalRead(VOICE_BUTTON_PIN) == HIGH) {
      last_voice_button_state = HIGH;
    }
  }
}

// Buzzer button on BUZZER_BUTTON_PIN (active-low): each press toggles the manual
// buzzer override. The environment alarm can still sound the buzzer independently.
void handleBuzzerButton() {
  int state = digitalRead(BUZZER_BUTTON_PIN);

  if (state == LOW && last_buzzer_button_state == HIGH) {
    delay(BUTTON_DEBOUNCE_MS);
    if (digitalRead(BUZZER_BUTTON_PIN) == LOW) {
      last_buzzer_button_state = LOW;
      buzzer_manual = !buzzer_manual;
      updateBuzzer();
      drawStatus(buzzer_manual ? "Buzzer ON" : "Buzzer OFF");
    }
    return;
  }

  if (state == HIGH && last_buzzer_button_state == LOW) {
    delay(BUTTON_DEBOUNCE_MS);
    if (digitalRead(BUZZER_BUTTON_PIN) == HIGH) {
      last_buzzer_button_state = HIGH;
    }
  }
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    drawStatus("WiFi Disconnected");
    WiFi.reconnect();
    delay(1000);
    connectWiFi();
  }

  if (!mqtt_client.connected()) {
    unsigned long now = millis();
    if (now - last_mqtt_reconnect_ms >= MQTT_RECONNECT_INTERVAL_MS) {
      last_mqtt_reconnect_ms = now;
      connectMqtt();
    }
  } else {
    mqtt_client.loop();
  }

  handleButton();
  handleVoiceButton();
  handleBuzzerButton();
  updateDht();
  updatePir();
  updateBuzzer();

  if (need_report && mqtt_client.connected()) {
    reportLedStatus(true);
  }

  if (mqtt_client.connected() && millis() - last_status_report_ms >= STATUS_REPORT_INTERVAL_MS) {
    reportLedStatus(false);
  }

  delay(10);
}
