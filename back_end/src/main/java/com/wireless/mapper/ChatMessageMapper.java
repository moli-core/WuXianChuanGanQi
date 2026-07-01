package com.wireless.mapper;

import com.wireless.model.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ChatMessageMapper {

    int insert(ChatMessage message);

    /** 按会话 ID 查询消息历史 */
    List<ChatMessage> selectBySession(@Param("sessionId") String sessionId,
                                      @Param("limit") Integer limit);

    /** 用户的消息历史列表 */
    List<ChatMessage> selectByUser(@Param("userId") Long userId,
                                   @Param("limit") Integer limit);

    /** 统计今日对话次数 */
    int countToday(@Param("startTime") LocalDateTime startTime,
                   @Param("endTime") LocalDateTime endTime);
}
