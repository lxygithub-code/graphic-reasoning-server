package com.lee.graphic_reasoning_server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lee.graphic_reasoning_server.po.Dict;
import com.lee.graphic_reasoning_server.vo.DictTypeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DictMapper extends BaseMapper<Dict> {
    /** 统计所有 dict_type 及其条目数 */
    @Select("SELECT dict_type AS dictType, COUNT(*) AS count " +
            "FROM t_dict WHERE deleted = 0 GROUP BY dict_type ORDER BY dict_type")
    List<DictTypeVO> selectTypes();
}