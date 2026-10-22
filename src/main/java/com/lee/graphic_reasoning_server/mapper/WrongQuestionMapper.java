package com.lee.graphic_reasoning_server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lee.graphic_reasoning_server.po.WrongQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface WrongQuestionMapper extends BaseMapper<WrongQuestion> {
    /** 统计错题类型分布（包含已被用户移除的错题） */
    /** 统计错题类型分布（按一级分类聚合） */
    @Select("SELECT SUBSTRING_INDEX(q.category, '/', 1) AS category, SUM(w.wrong_count) AS totalWrong " +
            "FROM t_wrong_question w JOIN t_question q ON w.question_id = q.id " +
            "WHERE q.deleted = 0 " +
            "GROUP BY SUBSTRING_INDEX(q.category, '/', 1) " +
            "ORDER BY totalWrong DESC")
    List<Map<String, Object>> selectWrongCategoryStats();

    /** 错题 TOP 10 排行（包含已被用户移除的错题） */
    @Select("SELECT w.question_id AS questionId, q.content AS content, q.category AS category, w.wrong_count AS wrongCount " +
            "FROM t_wrong_question w JOIN t_question q ON w.question_id = q.id " +
            "WHERE q.deleted = 0 " +
            "ORDER BY w.wrong_count DESC LIMIT 10")
    List<Map<String, Object>> selectTopWrongQuestions();
}