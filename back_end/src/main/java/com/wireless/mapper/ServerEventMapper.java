package com.wireless.mapper;

import com.wireless.model.entity.ServerEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ServerEventMapper {

    int insert(ServerEvent event);
    List<ServerEvent> selectByType(@Param("eventType") String eventType,
                                   @Param("limit") Integer limit);
    List<ServerEvent> selectByDevice(@Param("deviceCode") String deviceCode,
                                     @Param("limit") Integer limit);

    /** 事件趋势统计 */
    List<Map<String, Object>> countTrendByDay(@Param("days") int days);

    /** 事件类型分布统计 */
    List<Map<String, Object>> countByType(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /** 设备活跃排行 */
    List<Map<String, Object>> deviceActivityRanking(@Param("limit") Integer limit);

    /** 设备上报次数统计 */
    int countByDeviceAndType(@Param("deviceCode") String deviceCode,
                             @Param("eventType") String eventType,
                             @Param("startTime") LocalDateTime startTime,
                             @Param("endTime") LocalDateTime endTime);
}
