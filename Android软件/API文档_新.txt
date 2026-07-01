# AIOT 无线传感网后端 API 文档 v3.0

**Base URL:** `http://10.27.63.205:8080`
**WebSocket:** `ws://10.27.63.205:8080/ws/device-state`
**统一返回:** `{"code":200, "message":"success", "data":{...}, "timestamp":1700000000000}`

---

## 一、用户认证

### POST /api/auth/login
```json
请求 {"username":"admin","password":"admin"}
返回 {"code":200,"data":{"token":"eyJ...","userId":1,"username":"admin","nickname":"管理员","role":"admin"}}
```

### POST /api/auth/register
```json
请求 {"username":"test","password":"123456","nickname":"测试"}
返回 {"code":200,"data":"注册成功"}
```

---

## 二、设备控制

| 设备 | deviceCode | 接口 |
|------|-----------|------|
| 绿灯 | `led` | POST /api/device/control |
| 红灯 | `ledRed` | POST /api/device/control |
| 黄灯 | `ledYellow` | POST /api/device/control |
| 蜂鸣器 | `buzzer` | POST /api/device/control |

### POST /api/device/control — 控制单个设备
```json
请求 {"deviceCode":"led","action":1}
     action: 1=开 0=关
返回 {"code":200}
```

### POST /api/device/light — 控制绿灯（兼容硬件）
```json
请求 {"enabled":true}
返回 {"code":200,"data":{"controlCmd":{"service_id":"Esp32","properties":{"Status_LED":true}},"cloudSent":true}}
```

### POST /api/device/all-on — 一键全开（4设备同步）
### POST /api/device/all-off — 一键全关（4设备同步）
无请求体，一次 HTTP 全部下发。

### GET /api/device/status — 查所有设备状态
```json
返回 {"code":200,"data":[{"deviceCode":"led","deviceName":"绿灯","status":1},...]}
```

---

## 三、环境数据

### GET /api/data/dashboard — 仪表盘
```json
返回 {"code":200,"data":{"currentTemp":26,"currentHumidity":62,"currentSmoke":0,
       "deviceStatus":{"led":1,"ledRed":0,"ledYellow":1,"buzzer":0},
       "todayAlertCount":0,"todayVoiceCount":0}}
```

### GET /api/data/latest — 最新环境数据
### GET /api/data/chart?hours=24 — 近N小时曲线（24/168/720）

---

## 四、语音控制

### POST /api/voice/upload — 上传音频文件（小程序录音）
multipart/form-data, 字段名 `file`

### POST /api/voice/stream — 文字/音频流
请求体为文本或音频二进制

### POST /api/voice/light — 语音控制灯（兼容硬件ESP32）
multipart/form-data, 字段名 `audio`

返回格式（三个接口统一）：
```json
{"code":200,"data":{"voiceText":"打开灯","controlCmd":{"service_id":"Esp32","properties":{"Status_LED":true}},"cloudSent":true}}
```

---

## 五、AI 对话

### POST /api/ai/chat — 智能问答（DeepSeek）
```json
请求 {"question":"当前环境怎么样？"}
返回 {"code":200,"data":{"question":"...","reply":"当前温度26°C，湿度62%，环境正常。"}}
```

### POST /api/ai/compose — 组词成句（兼容硬件）
```json
请求 {"text":"电风扇 打开 有点热"}
返回 {"code":200,"data":{"input":"...","result":"有点热，请打开电风扇。","model":"deepseek-chat"}}
```

---

## 六、天气 & 地图

### GET /api/weather/current
### GET /api/weather/city
### POST /api/map/route
```json
请求 {"origin":"天安门","destination":"鸟巢"}
```
### GET /api/map/search?keyword=xxx

---

## 七、AIOT 设备管理

### GET /api/aiot-device — 设备列表
可选筛选 `?keyword=xxx&deviceType=esp32&onlineStatus=1`

### GET /api/aiot-device/{deviceCode} — 设备详情（含完整状态字段）
### POST /api/aiot-device — 注册设备
```json
请求 {"deviceCode":"esp32_001","deviceName":"客厅设备","deviceType":"esp32","ipAddress":"10.27.x.x"}
```
### PUT /api/aiot-device/{id} — 修改
### DELETE /api/aiot-device/{id} — 删除
### POST /api/aiot-device/{deviceCode}/command?command=led:on — 下发指令

---

## 八、服务器管理

### GET /api/server — 列表
### POST /api/server — 创建
### PUT /api/server/{id} — 修改
### DELETE /api/server/{id} — 删除
### POST /api/server/{id}/start — 启动
### POST /api/server/{id}/stop — 停止
### GET /api/server/events — 事件日志（device_online/offline/report等）

---

## 九、模型引擎

### GET /api/model/config — 模型列表
### POST /api/model/config — 新增
### PUT /api/model/config/{id} — 修改
### DELETE /api/model/config/{id} — 删除
### PUT /api/model/default/{id} — 设为默认
### POST /api/model/test?message=xxx — 测试模型

---

## 十、数据报表

### GET /api/report/device-stats — 设备统计
```json
返回 {"totalDevices":5,"onlineDevices":1,"todayVoiceCount":15,"todayAlertCount":2}
```
### GET /api/report/event-trends?days=30 — 事件趋势（折线图）
### GET /api/report/event-distribution — 事件分布（环图）
### GET /api/report/device-ranking?limit=10 — 活跃排行（柱状图）
### GET /api/report/message-history — 消息历史

---

## 十一、日志

### GET /api/log/voice — 语音记录
### GET /api/log/operation?deviceCode=led — 操作记录
### GET /api/log/alert — 告警记录
### PUT /api/log/alert/{id}/handle — 标记已处理

---

## WebSocket 实时推送

**地址:** `ws://10.27.63.205:8080/ws/device-state`

推送格式（设备开关变化时即时推送，温湿度每5秒）：
```json
{"Status_LED":true, "Status_beeper":false, "Data_temp":26, "Data_humi":62, "Status_body":0}
```
