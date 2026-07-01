package com.wireless.mapper;

import com.wireless.model.entity.AiModelConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface AiModelConfigMapper {

    int insert(AiModelConfig config);
    int update(AiModelConfig config);
    int deleteById(@Param("id") Long id);

    AiModelConfig selectById(@Param("id") Long id);
    List<AiModelConfig> selectAll();

    /** 获取默认启用的模型 */
    AiModelConfig selectDefault();

    /** 取消所有默认 */
    int clearDefault();

    /** 设置为默认 */
    int setDefault(@Param("id") Long id);
}
