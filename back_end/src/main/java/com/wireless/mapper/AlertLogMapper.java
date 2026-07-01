package com.wireless.mapper;

import com.wireless.model.entity.AlertLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警日志 Mapper
 */
@Mapper
public interface AlertLogMapper {

    /** 插入告警记录 */
    int insert(AlertLog alertLog);

    /** 按类型查询 */
    List<AlertLog> selectByType(@Param("alertType") String alertType);

    /** 按时间范围查询 */
    List<AlertLog> selectByTimeRange(@Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);

    /** 统计今日告警次数 */
    int countToday(@Param("startOfDay") LocalDateTime startOfDay,
                   @Param("endOfDay") LocalDateTime endOfDay);

    /** 标记告警已处理 */
    int markHandled(@Param("id") Long id);
}
