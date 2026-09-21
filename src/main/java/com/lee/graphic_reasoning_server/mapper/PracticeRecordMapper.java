package com.lee.graphic_reasoning_server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lee.graphic_reasoning_server.po.PracticeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PracticeRecordMapper extends BaseMapper<PracticeRecord> {
    /**
     * 全表平均正确率（用于仪表盘统计）
     */
    @Select("SELECT AVG(accuracy) FROM t_practice_record")
    Double selectAvgAccuracy();
}
