// pages/index/index.js
const app = getApp()

Page({
  data: {
    uid: "30bb2e98359643078a1f1f35f3e579d5",
    ledtopic: "led002",

    device_status: "在线",
    ledOnOff: "关闭",
    checked: false,
    ledicon: "/utils/img/lightoff.png",
    client: null,
    recorderManager: null,
    inputText: "",
    isRecording: false
  },

  onLoad() {
    console.log("=== 页面加载 ===");
    
    // 初始化录音管理器
    const recorderManager = wx.getRecorderManager();
    this.setData({
      recorderManager: recorderManager,
      isRecording: false
    });

    // 录音结束回调
    recorderManager.onStop((res) => {
      console.log("录音结束");
      this.setData({ isRecording: false });
      
      const tempAudioPath = res.tempFilePath;
      console.log("录音文件路径：", tempAudioPath);
      console.log("录音时长：", res.duration, "ms");
      
      if (res.duration < 1200) {
        wx.showToast({ 
          title: "录音时间太短，请重新录音", 
          icon: "none" 
        });
        return;
      }
      
      this.uploadAudio(tempAudioPath);
    });

    // 录音开始回调
    recorderManager.onStart(() => {
      console.log("录音开始");
      this.setData({ isRecording: true });
    });

    // 录音错误回调
    recorderManager.onError((err) => {
      console.error("录音错误：", err);
      this.setData({ isRecording: false });
      wx.showToast({ 
        title: "录音失败：" + err.errMsg, 
        icon: "none" 
      });
    });
  },

  // ===== 输入框事件 =====
  onInputChange(event) {
    this.setData({
      inputText: event.detail.value
    });
  },

  // ===== 发送文字指令 =====
  sendTextCommand() {
    const text = this.data.inputText.trim();
    if (!text) {
      wx.showToast({ title: "请输入控制指令", icon: "none" });
      return;
    }
    
    wx.showLoading({ title: "指令解析中..." });
    
    wx.request({
      url: "http://127.0.0.1:8080/api/text/light",
      method: "POST",
      header: {
        "Content-Type": "application/json"
      },
      data: {
        text: text
      },
      success: (res) => {
        wx.hideLoading();
        console.log("文字指令响应：", res.data);
        
        if (res.data.success) {
          const cmd = res.data.data.controlCmd;
          const inputText = res.data.data.inputText;
          wx.showToast({
            title: `指令 "${inputText}" 已发送`,
            icon: "success"
          });

          this.updateLightStatus(cmd);
          this.setData({ inputText: "" });
        } else {
          wx.showToast({
            title: res.data.msg || "指令解析失败",
            icon: "none"
          });
        }
      },
      fail: (err) => {
        wx.hideLoading();
        console.error("请求失败：", err);
        wx.showToast({ 
          title: "接口请求失败", 
          icon: "error" 
        });
      }
    });
  },

  // ===== 语音控制 - 长按开始录音 =====
  startRecord() {
    console.log("=== 开始录音 ===");
    
    if (this.data.isRecording) {
      console.log("已在录音中");
      return;
    }
    
    const recorder = this.data.recorderManager;
    if (!recorder) {
      console.error("录音管理器未初始化");
      wx.showToast({ 
        title: "录音管理器未初始化", 
        icon: "error" 
      });
      return;
    }
    
    wx.showToast({ 
      title: "开始录音...", 
      icon: "none" 
    });
    
    recorder.start({
      duration: 60000,
      format: "mp3",
      sampleRate: 16000,
      numberOfChannels: 1,
      encodeBitRate: 48000
    });
  },

  // ===== 语音控制 - 松开停止录音 =====
  stopRecord() {
    console.log("=== 停止录音 ===");
    
    if (!this.data.isRecording) {
      console.log("未在录音中");
      return;
    }
    
    const recorder = this.data.recorderManager;
    if (!recorder) {
      this.setData({ isRecording: false });
      return;
    }
    
    wx.showToast({ 
      title: "录音结束，识别中...", 
      icon: "none" 
    });
    
    recorder.stop();
  },

  // ===== 上传音频文件 =====
  uploadAudio(audioPath) {
    wx.showLoading({ 
      title: "语音识别中...", 
      mask: true 
    });
    
    wx.uploadFile({
      url: "http://127.0.0.1:8080/api/voice/light",
      filePath: audioPath,
      name: "audio",
      header: {
        "Content-Type": "multipart/form-data"
      },
      formData: {},
      success: (res) => {
        wx.hideLoading();
        console.log("语音识别响应：", res.data);
        
        try {
          const result = JSON.parse(res.data);
          
          if (result.success) {
            const voiceText = result.data.voiceText || "未知";
            const cmd = result.data.controlCmd || "";
            
            wx.showToast({ 
              title: `识别: ${voiceText}`, 
              icon: "success",
              duration: 2000
            });
            
            this.updateLightStatus(cmd);
          } else {
            wx.showToast({ 
              title: result.msg || "语音识别失败", 
              icon: "none" 
            });
          }
        } catch (e) {
          console.error("解析响应失败：", e);
          wx.showToast({ 
            title: "服务器响应异常", 
            icon: "error" 
          });
        }
      },
      fail: (err) => {
        wx.hideLoading();
        console.error("音频上传失败：", err);
        wx.showToast({ 
          title: "音频上传失败", 
          icon: "error" 
        });
      }
    });
  },

  // ===== 统一更新灯光状态 =====
  updateLightStatus(cmd) {
    if (cmd === "on") {
      this.setData({
        checked: true,
        ledOnOff: "开启",
        ledicon: "/utils/img/lighton.png"
      });
      this.LedSendMsg("on");
    } else if (cmd === "off") {
      this.setData({
        checked: false,
        ledOnOff: "关闭",
        ledicon: "/utils/img/lightoff.png"
      });
      this.LedSendMsg("off");
    } else if (cmd && !isNaN(cmd)) {
      console.log("亮度指令：", cmd);
      wx.showToast({ 
        title: `亮度设置为 ${cmd}%`, 
        icon: "none" 
      });
      this.LedSendMsg(cmd);
    }
  },

  // ===== 滑块开关 =====
  onChange({ detail }) {
    const isOn = detail;
    this.setData({ checked: isOn });
    
    if (isOn) {
      this.setData({ 
        ledicon: "/utils/img/lighton.png", 
        ledOnOff: "开启" 
      });
      this.LedSendMsg("on");
    } else {
      this.setData({ 
        ledicon: "/utils/img/lightoff.png", 
        ledOnOff: "关闭" 
      });
      this.LedSendMsg("off");
    }
  },

  // ===== 点击图标切换 =====
  onChange2() {
    const isOn = !this.data.checked;
    this.setData({ 
      checked: isOn,
      ledicon: isOn ? "/utils/img/lighton.png" : "/utils/img/lightoff.png",
      ledOnOff: isOn ? "开启" : "关闭"
    });
    this.LedSendMsg(isOn ? "on" : "off");
  },

  // ===== 打开灯光按钮 =====
  turnLightOn() {
    this.setData({
      checked: true,
      ledOnOff: "开启",
      ledicon: "/utils/img/lighton.png"
    });
    this.LedSendMsg("on");
  },

  // ===== 关闭灯光按钮 =====
  turnLightOff() {
    this.setData({
      checked: false,
      ledOnOff: "关闭",
      ledicon: "/utils/img/lightoff.png"
    });
    this.LedSendMsg("off");
  },

  // ===== MQTT 指令推送 =====
  LedSendMsg(msg) {
    wx.request({
      url: "https://apis.bemfa.com/va/postJsonMsg",
      method: "POST",
      header: {
        "Content-Type": "application/json; charset=utf-8"
      },
      data: {
        uid: this.data.uid,
        topic: this.data.ledtopic,
        type: 1,
        msg: msg,
        temp: 25.6,
        humi: 48
      },
      success: (res) => {
        console.log("MQTT 推送回执：", res.data);
      },
      fail: (err) => {
        console.error("MQTT 推送失败：", err);
      }
    });
  },

  // ===== 页面卸载时清理 =====
  onUnload() {
    console.log("=== 页面卸载 ===");
    if (this.data.isRecording) {
      const recorder = this.data.recorderManager;
      if (recorder) {
        try {
          recorder.stop();
        } catch (err) {
          console.error("停止录音异常：", err);
        }
      }
    }
    this.setData({ isRecording: false });
  }
});