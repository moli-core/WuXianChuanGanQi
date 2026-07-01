package com.wireless.mapper;

import com.wireless.model.entity.AiotDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface AiotDeviceMapper {

    int insert(AiotDevice device);
    int update(AiotDevice device);
    int deleteById(@Param("id") Long id);

    AiotDevice selectByDeviceCode(@Param("deviceCode") String deviceCode);
    AiotDevice selectById(@Param("id") Long id);
    List<AiotDevice> selectAll(@Param("keyword") String keyword,
                               @Param("deviceType") String deviceType,
                               @Param("onlineStatus") Integer onlineStatus);

    /** 设备自动注册 or Upsert */
    int upsertOnReport(AiotDevice device);

    /** 更新设备实时状态（收到 MQTT 上报时） */
    int updateReportState(AiotDevice device);

    int countAll();
    int countOnline();

    /** 更新设备在线状态 */
    int updateOnlineStatus(@Param("deviceCode") String deviceCode,
                           @Param("onlineStatus") Integer onlineStatus);
}
