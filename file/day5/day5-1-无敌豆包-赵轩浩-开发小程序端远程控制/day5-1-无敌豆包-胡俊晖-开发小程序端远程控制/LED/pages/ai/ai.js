// pages/ai-assistant/ai.js
Page({
  data: {
    // 当前模式：smart-智能搜索，deep-深度搜索
    currentMode: 'smart',
    
    // 消息列表
    messages: [],
    
    // 输入内容
    inputMessage: '',
    
    // 是否正在打字
    isTyping: false,
    
    // 滚动定位
    scrollToView: '',
    
    // 用户信息
    userInfo: {
      avatar: ''
    },
    
    // 深度搜索相关
    selectedDevice: '',
    deviceTypes: ['照明设备', '智能插座', '智能网关', '安防设备', '环境监测'],
    
    // 会话ID
    sessionId: '',
    
    // 后端接口地址
    baseUrl: 'http://127.0.0.1:8080/api/smarthome/qa'
  },

  onLoad(options) {
    // 生成会话ID
    this.setData({
      sessionId: this.generateSessionId()
    });
    
    // 初始化欢迎消息
    this.showWelcomeMessage();
  },

  /**
   * 显示欢迎消息
   */
  showWelcomeMessage() {
    const { currentMode } = this.data;
    
    let welcomeContent = currentMode === 'smart' 
      ? '👋 您好！我是智能搜索助手，可以快速回答您的问题。\n\n💡 试试问我：\n• 如何连接智能灯泡？\n• 智能家居如何节能？\n• 网关离线怎么办？'
      : '👋 您好！我是深度搜索助手，将基于智能家居知识库为您提供精准答案。\n\n📚 支持：\n• 设备操作指南\n• 故障排查\n• 场景联动设置\n• 兼容性查询';
    
    this.setData({
      messages: [{
        id: 'welcome',
        role: 'assistant',
        content: welcomeContent,
        mode: currentMode,
        time: this.getCurrentTime()
      }]
    });
  },

  /**
   * 切换模式
   */
  switchMode(e) {
    const mode = e.currentTarget.dataset.mode;
    if (mode === this.data.currentMode) return;
    
    // 清空对话
    this.setData({
      currentMode: mode,
      messages: [],
      inputMessage: '',
      selectedDevice: '',
      sessionId: this.generateSessionId()
    });
    
    // 显示欢迎消息
    this.showWelcomeMessage();
    
    // 触觉反馈
    wx.vibrateShort({ type: 'light' });
    
    wx.showToast({
      title: `已切换到${mode === 'smart' ? '智能搜索' : '深度搜索'}模式`,
      icon: 'success',
      duration: 1500
    });
  },

  /**
   * 选择设备类型（深度搜索）
   */
  selectDevice(e) {
    const device = e.currentTarget.dataset.device;
    this.setData({
      selectedDevice: device
    });
    
    wx.showToast({
      title: `已筛选：${device || '全部设备'}`,
      icon: 'success',
      duration: 1000
    });
  },

  /**
   * 输入框内容变化
   */
  onInput(e) {
    this.setData({
      inputMessage: e.detail.value
    });
  },

  /**
   * 发送消息
   */
  async sendMessage() {
    const { inputMessage, currentMode, isTyping, selectedDevice, sessionId } = this.data;
    
    if (!inputMessage || inputMessage.trim() === '') return;
    if (isTyping) return;

    const question = inputMessage.trim();

    // 添加用户消息
    this.addMessage({
      role: 'user',
      content: question,
      mode: currentMode,
      time: this.getCurrentTime()
    });

    // 清空输入框
    this.setData({
      inputMessage: '',
      isTyping: true
    });

    // 添加占位消息
    const tempId = 'temp_' + Date.now();
    const messageIndex = this.data.messages.length;
    
    this.addMessage({
      id: tempId,
      role: 'assistant',
      content: currentMode === 'smart' ? '🤔 思考中...' : '🔍 深度搜索中，正在匹配知识库...',
      mode: currentMode,
      isLoading: true,
      time: this.getCurrentTime()
    });

    try {
      let response;
      
      if (currentMode === 'smart') {
        // ========== 智能搜索模式 ==========
        response = await this.smartSearch(question);
      } else {
        // ========== 深度搜索模式 ==========
        response = await this.deepSearch(question, selectedDevice);
      }

      // 更新消息
      this.updateMessage(messageIndex, {
        content: response.answer || '抱歉，没有获取到答案',
        isLoading: false,
        sources: response.sources || [],
        matchedCount: response.matchedCount || 0,
        bestMatch: response.bestMatch || '',
        processingTime: response.processingTimeMs || 0,
        showSources: false,
        mode: currentMode,
        time: this.getCurrentTime()
      });

      // 深度搜索显示匹配提示
      if (currentMode === 'deep' && response.matchedCount > 0) {
        wx.showToast({
          title: `✅ 匹配到 ${response.matchedCount} 条相关知识`,
          icon: 'success',
          duration: 2000
        });
      }

    } catch (error) {
      console.error('问答失败：', error);
      
      this.updateMessage(messageIndex, {
        content: '😅 抱歉，服务暂时不可用，请稍后重试\n\n💡 提示：请检查网络连接或联系客服',
        isLoading: false,
        isError: true,
        mode: currentMode,
        time: this.getCurrentTime()
      });
    }

    this.setData({
      isTyping: false
    });
  },

  /**
   * 智能搜索（快速问答）
   */
  smartSearch(question) {
    return new Promise((resolve, reject) => {
      const { baseUrl, sessionId } = this.data;
      
      wx.request({
        url: `${baseUrl}/ask`,
        method: 'POST',
        data: {
          question: question,
          deviceType: '',
          sessionId: sessionId
        },
        header: {
          'Content-Type': 'application/json'
        },
        timeout: 30000,
        success: (res) => {
          if (res.statusCode === 200) {
            resolve(res.data);
          } else {
            reject(new Error(res.data?.error || '请求失败'));
          }
        },
        fail: (err) => {
          reject(err);
        }
      });
    });
  },

  /**
   * 深度搜索（知识库匹配）
   */
  deepSearch(question, deviceType) {
    return new Promise((resolve, reject) => {
      const { baseUrl, sessionId } = this.data;
      
      wx.request({
        url: `${baseUrl}/ask/exact`,
        method: 'POST',
        data: {
          question: question,
          deviceType: deviceType || '',
          sessionId: sessionId
        },
        header: {
          'Content-Type': 'application/json'
        },
        timeout: 30000,
        success: (res) => {
          if (res.statusCode === 200) {
            resolve(res.data);
          } else {
            reject(new Error(res.data?.error || '请求失败'));
          }
        },
        fail: (err) => {
          reject(err);
        }
      });
    });
  },

  /**
   * 添加消息
   */
  addMessage(message) {
    const messages = this.data.messages;
    const newMessage = {
      id: message.id || Date.now() + '_' + Math.random().toString(36).substr(2, 9),
      ...message
    };
    messages.push(newMessage);
    
    this.setData({
      messages,
      scrollToView: 'msg-' + newMessage.id
    });
    
    return messages.length - 1;
  },

  /**
   * 更新消息
   */
  updateMessage(index, updates) {
    const messages = this.data.messages;
    if (messages[index]) {
      messages[index] = { ...messages[index], ...updates };
      this.setData({
        messages,
        scrollToView: 'msg-' + messages[index].id
      });
    }
  },

  /**
   * 复制消息
   */
  copyMessage(e) {
    const content = e.currentTarget.dataset.content;
    wx.setClipboardData({
      data: content,
      success: () => {
        wx.showToast({
          title: '已复制',
          icon: 'success'
        });
      }
    });
  },

  /**
   * 切换显示来源（深度搜索）
   */
  toggleSources(e) {
    const id = e.currentTarget.dataset.id;
    const messages = this.data.messages;
    const index = messages.findIndex(m => m.id === id);
    
    if (index !== -1) {
      messages[index].showSources = !messages[index].showSources;
      this.setData({ messages });
    }
  },

  /**
   * 清空历史
   */
  clearHistory() {
    wx.showModal({
      title: '提示',
      content: '确定要清空所有对话记录吗？',
      success: (res) => {
        if (res.confirm) {
          this.setData({
            messages: [],
            sessionId: this.generateSessionId()
          });
          this.showWelcomeMessage();
          
          wx.showToast({
            title: '已清空',
            icon: 'success'
          });
        }
      }
    });
  },

  /**
   * 获取当前时间
   */
  getCurrentTime() {
    const now = new Date();
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    return `${hours}:${minutes}`;
  },

  /**
   * 生成会话ID
   */
  generateSessionId() {
    return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
  },

  /**
   * 页面分享
   */
  onShareAppMessage() {
    return {
      title: 'AI智能助手',
      path: '/pages/ai-assistant/ai-assistant',
      imageUrl: '/images/share.png'
    };
  }
});