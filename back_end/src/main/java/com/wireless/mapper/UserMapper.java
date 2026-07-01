package com.wireless.mapper;

import com.wireless.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper {

    /** 根据用户名查询 */
    User selectByUsername(@Param("username") String username);

    /** 根据 ID 查询 */
    User selectById(@Param("id") Long id);

    /** 插入用户 */
    int insert(User user);

    /** 更新用户 */
    int update(User user);

    /** 删除用户 */
    int deleteById(@Param("id") Long id);

    /** 禁用/启用用户 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 用户列表 */
    List<User> selectAll(@Param("keyword") String keyword, @Param("role") String role);

    /** 更新最后登录时间 */
    int updateLastLogin(@Param("id") Long id);
}
