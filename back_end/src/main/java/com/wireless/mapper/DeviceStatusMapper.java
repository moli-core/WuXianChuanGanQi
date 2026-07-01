package com.wireless.mapper;

import com.wireless.model.entity.DeviceStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 设备状态 Mapper
 */
@Mapper
public interface DeviceStatusMapper {

    /** 查询所有设备状态 */
    List<DeviceStatus> selectAll();

    /** 根据设备编码查询 */
    DeviceStatus selectByDeviceCode(@Param("deviceCode") String deviceCode);

    /** 更新设备状态 */
    int updateStatus(@Param("deviceCode") String deviceCode,
                     @Param("status") Integer status);

    /** 初始化设备 (不存在则插入) */
    int insertIfNotExists(DeviceStatus deviceStatus);
}
