package com.lee.graphic_reasoning_server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lee.graphic_reasoning_server.po.PracticeDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PracticeDetailMapper extends BaseMapper<PracticeDetail> {

    /** 批量插入（一次练习明细较多，用批量提升性能） */
    int batchInsert(@Param("list") List<PracticeDetail> list);
}