package com.wireless.mapper;

import com.wireless.model.entity.EnvData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 环境数据 Mapper
 */
@Mapper
public interface EnvDataMapper {

    /** 插入一条环境数据 */
    int insert(EnvData envData);

    /** 查询最新一条环境数据 */
    EnvData selectLatest();

    /** 按时间范围查询环境数据 */
    List<EnvData> selectByTimeRange(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    /** 查询过去 N 小时的数据 (用于图表) */
    List<EnvData> selectRecentHours(@Param("hours") int hours);

    /** 获取时间段内的平均值 */
    BigDecimal selectAvgTemp(@Param("startTime") LocalDateTime startTime,
                             @Param("endTime") LocalDateTime endTime);
}
