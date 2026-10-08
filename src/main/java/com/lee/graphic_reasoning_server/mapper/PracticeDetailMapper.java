package com.lee.graphic_reasoning_server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lee.graphic_reasoning_server.po.PracticeDetail;
import com.lee.graphic_reasoning_server.vo.QuestionDifficultyStatVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PracticeDetailMapper extends BaseMapper<PracticeDetail> {

    /**
     * 批量插入（一次练习明细较多，用批量提升性能）
     */
    int batchInsert(@Param("list") List<PracticeDetail> list);

    /**
     * 按题目聚合作答明细
     * 只返回累计作答 >= minCount 的题目
     */
    @Select("""
                SELECT question_id AS questionId,
                       COUNT(*) AS totalCount,
                       SUM(CASE WHEN is_correct = 1 THEN 1 ELSE 0 END) AS correctCount
                FROM t_practice_detail
                GROUP BY question_id
                HAVING COUNT(*) >= #{minCount}
            """)
    List<QuestionDifficultyStatVO> selectDifficultyStats(@Param("minCount") int minCount);
}