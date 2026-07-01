package com.wireless.mapper;

import com.wireless.model.entity.ServerInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ServerInstanceMapper {

    int insert(ServerInstance server);
    int update(ServerInstance server);
    int deleteById(@Param("id") Long id);

    ServerInstance selectById(@Param("id") Long id);
    List<ServerInstance> selectAll();

    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
