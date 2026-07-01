package com.wireless.service;

import com.wireless.model.entity.EnvData;
import com.wireless.model.vo.DashboardVO;
import com.wireless.model.vo.EnvChartVO;

/**
 * 环境数据服务
 */
public interface EnvDataService {

    /** 保存环境数据 */
    void saveEnvData(EnvData envData);

    /** 获取最新环境数据 */
    EnvData getLatest();

    /** 获取仪表盘数据 */
    DashboardVO getDashboard();

    /** 获取图表数据 (近 N 小时) */
    EnvChartVO getChartData(int hours);
}
