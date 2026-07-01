package com.wireless.mapper;

import com.wireless.model.entity.VoiceLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 语音日志 Mapper
 */
@Mapper
public interface VoiceLogMapper {

    /** 插入语音日志 */
    int insert(VoiceLog voiceLog);

    /** 查询所有语音日志 */
    List<VoiceLog> selectAll();

    /** 按时间范围查询 */
    List<VoiceLog> selectByTimeRange(@Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);

    /** 统计今日语音指令次数 */
    int countToday(@Param("startOfDay") LocalDateTime startOfDay,
                   @Param("endOfDay") LocalDateTime endOfDay);

    /** 统计有效指令比例 */
    int countValid(@Param("startTime") LocalDateTime startTime,
                   @Param("endTime") LocalDateTime endTime);
}
