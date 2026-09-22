package com.lee.graphic_reasoning_server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lee.graphic_reasoning_server.po.Question;
import com.lee.graphic_reasoning_server.vo.ExamTypeCountVO;
import com.lee.graphic_reasoning_server.vo.QuestionSourceStatVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    @Select("<script>" +
            "SELECT q.id FROM t_question q " +
            "WHERE q.deleted = 0 " +
            "<if test='category != null and category != \"\"'>AND q.category = #{category}</if> " +
            "<if test='examType != null and examType != \"\"'>AND q.exam_type = #{examType}</if> " +
            "<if test='examSubType != null and examSubType != \"\"'>AND q.exam_sub_type = #{examSubType}</if> " +
            "AND NOT EXISTS (SELECT 1 FROM t_user_question uq " +
            "                WHERE uq.user_id = #{userId} AND uq.question_id = q.id) " +
            "ORDER BY RAND() LIMIT #{count}" +
            "</script>")
    List<Long> randomUnknownIds(@Param("userId") Long userId,
                                @Param("category") String category,
                                @Param("examType") String examType,
                                @Param("examSubType") String examSubType,
                                @Param("count") int count);

    @Select("<script>" +
            "SELECT source, " +
            "MAX(exam_type) AS examType, " +
            "MAX(exam_sub_type) AS examSubType, " +
            "COUNT(*) AS count " +
            "FROM t_question " +
            "WHERE deleted = 0 AND source IS NOT NULL AND source != '' " +
            "<if test='examType != null and examType != \"\"'>AND exam_type = #{examType}</if> " +
            "<if test='examSubType != null and examSubType != \"\"'>AND exam_sub_type = #{examSubType}</if> " +
            "<if test='keyword != null and keyword != \"\"'>AND source LIKE CONCAT('%', #{keyword}, '%')</if> " +
            "GROUP BY source " +
            "ORDER BY source DESC" +
            "</script>")
    List<QuestionSourceStatVO> selectSourceStats(@Param("examType") String examType,
                                                 @Param("examSubType") String examSubType,
                                                 @Param("keyword") String keyword);

    @Select("SELECT exam_type AS examType, COUNT(*) AS count " +
            "FROM t_question " +
            "WHERE deleted = 0 " +
            "GROUP BY exam_type")
    List<ExamTypeCountVO> selectCountByExamType();
}