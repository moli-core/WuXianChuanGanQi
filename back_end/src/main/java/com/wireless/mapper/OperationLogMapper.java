package com.wireless.mapper;

import com.wireless.model.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志 Mapper
 */
@Mapper
public interface OperationLogMapper {

    /** 插入操作日志 */
    int insert(OperationLog operationLog);

    /** 按时间范围查询 */
    List<OperationLog> selectByTimeRange(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /** 按设备编码查询 */
    List<OperationLog> selectByDeviceCode(@Param("deviceCode") String deviceCode);

    /** 统计设备操作次数 */
    int countByDeviceCode(@Param("deviceCode") String deviceCode,
                          @Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime);
}
