// pages/profile/profile.js
Page({
  data: {
    userInfo: {
      avatar: '',
      nickname: '用户',
      phone: '未绑定手机号'
    },
    stats: {
      devices: 3,
      scenes: 5,
      logs: 12
    }
  },

  onLoad(options) {
    // 从本地存储获取用户信息
    const userInfo = wx.getStorageSync('userInfo') || {};
    this.setData({
      userInfo: userInfo
    });
  },

  onShow() {
    // 每次页面显示时刷新数据
    const userInfo = wx.getStorageSync('userInfo') || {};
    this.setData({
      userInfo: userInfo
    });
  },

  /**
   * 编辑个人信息
   */
  editProfile() {
    wx.showToast({
      title: '功能开发中',
      icon: 'none'
    });
  },

  /**
   * 页面跳转
   */
  navigateTo(e) {
    const url = e.currentTarget.dataset.url;
    if (url) {
      wx.navigateTo({
        url: url,
        fail: () => {
          wx.showToast({
            title: '页面开发中',
            icon: 'none'
          });
        }
      });
    }
  },

  /**
   * 退出登录
   */
  logout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          // 清除用户信息
          wx.removeStorageSync('userInfo');
          wx.removeStorageSync('token');
          
          wx.showToast({
            title: '已退出',
            icon: 'success'
          });

          // 跳转到登录页
          wx.reLaunch({
            url: '/pages/login/login'
          });
        }
      }
    });
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    // 刷新数据
    const userInfo = wx.getStorageSync('userInfo') || {};
    this.setData({
      userInfo: userInfo
    });
    wx.stopPullDownRefresh();
  }
});