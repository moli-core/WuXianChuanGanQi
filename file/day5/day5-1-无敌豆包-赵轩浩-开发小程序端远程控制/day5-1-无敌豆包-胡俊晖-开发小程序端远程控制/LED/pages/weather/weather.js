// pages/weather/weather.js
Page({
  data: {
    // 城市信息
    city: '成都',
    date: '2026年6月25日 星期四',
    
    // 当前天气
    temperature: 28,
    description: '多云',
    humidity: 65,
    windSpeed: 3,
    visibility: 10,
    airQuality: '良',
    airQualityLevel: '二级',
    
    // 天气图标（根据天气显示不同表情）
    weatherIcon: '⛅',
    
    // 未来7天天气预报
    forecast: [
      { day: '今天', icon: '⛅', temp: '28°', high: 31, low: 22 },
      { day: '周五', icon: '☀️', temp: '30°', high: 33, low: 23 },
      { day: '周六', icon: '☀️', temp: '32°', high: 35, low: 24 },
      { day: '周日', icon: '🌤️', temp: '29°', high: 31, low: 22 },
      { day: '周一', icon: '🌧️', temp: '24°', high: 26, low: 20 },
      { day: '周二', icon: '⛅', temp: '26°', high: 28, low: 21 },
      { day: '周三', icon: '☀️', temp: '31°', high: 34, low: 23 }
    ],
    
    // 生活指数
    lifeIndex: [
      { name: '穿衣指数', value: '舒适', desc: '建议穿T恤、短裤' },
      { name: '紫外线', value: '中等', desc: '建议涂抹防晒霜' },
      { name: '洗车指数', value: '适宜', desc: '天气不错，适合洗车' },
      { name: '运动指数', value: '适宜', desc: '适合户外运动' },
      { name: '感冒指数', value: '低发', desc: '感冒几率较低' },
      { name: '交通指数', value: '良好', desc: '天气对交通影响较小' }
    ],
    
    // 空气质量详情
    airDetail: {
      pm25: 35,
      pm10: 58,
      so2: 8,
      no2: 32,
      co: 0.6,
      o3: 72
    }
  },

  onLoad(options) {
    console.log('天气页面加载');
    // 可以在这里更新日期
    this.updateDateTime();
  },

  onShow() {
    // 每次显示时更新日期
    this.updateDateTime();
  },

  /**
   * 更新日期时间
   */
  updateDateTime() {
    const now = new Date();
    const weekdays = ['日', '一', '二', '三', '四', '五', '六'];
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const weekday = weekdays[now.getDay()];
    
    this.setData({
      date: `${year}年${month}月${day}日 星期${weekday}`
    });
  },

  /**
   * 刷新天气数据（模拟）
   */
  refreshWeather() {
    wx.showToast({
      title: '已刷新',
      icon: 'success'
    });
  },

  /**
   * 切换城市（模拟）
   */
  switchCity() {
    const cities = ['成都', '北京', '上海', '深圳', '杭州'];
    const currentIndex = cities.indexOf(this.data.city);
    const nextIndex = (currentIndex + 1) % cities.length;
    const newCity = cities[nextIndex];
    
    // 模拟不同城市的不同天气
    const weatherData = {
      '成都': { temp: 28, desc: '多云', icon: '⛅', humidity: 65 },
      '北京': { temp: 32, desc: '晴', icon: '☀️', humidity: 40 },
      '上海': { temp: 26, desc: '小雨', icon: '🌧️', humidity: 80 },
      '深圳': { temp: 30, desc: '多云', icon: '⛅', humidity: 70 },
      '杭州': { temp: 27, desc: '阴天', icon: '☁️', humidity: 75 }
    };
    
    const data = weatherData[newCity];
    
    this.setData({
      city: newCity,
      temperature: data.temp,
      description: data.desc,
      weatherIcon: data.icon,
      humidity: data.humidity
    });
    
    wx.showToast({
      title: `已切换至${newCity}`,
      icon: 'success'
    });
  },

  /**
   * 查看详情
   */
  showDetail(e) {
    const index = e.currentTarget.dataset.index;
    const item = this.data.forecast[index];
    if (item) {
      wx.showModal({
        title: `${item.day} 天气详情`,
        content: `天气：${item.icon} ${item.temp}\n高温：${item.high}°C\n低温：${item.low}°C`,
        showCancel: false
      });
    }
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    this.refreshWeather();
    wx.stopPullDownRefresh();
  },

  /**
   * 分享
   */
  onShareAppMessage() {
    return {
      title: `今天${this.data.city}天气${this.data.temperature}°C，${this.data.description}`,
      path: '/pages/weather/weather'
    };
  }
});