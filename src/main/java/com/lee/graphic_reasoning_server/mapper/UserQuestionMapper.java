package com.lee.graphic_reasoning_server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lee.graphic_reasoning_server.po.UserQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserQuestionMapper extends BaseMapper<UserQuestion> {

    /** 抽未答题 */
    @Select("<script>" +
            "SELECT q.id FROM t_question q " +
            "WHERE q.deleted = 0 " +
            "<if test='category != null and category != \"\"'>AND q.category = #{category}</if> " +
            "<if test='examType != null and examType != \"\"'>AND q.exam_type = #{examType}</if> " +
            "AND NOT EXISTS (SELECT 1 FROM t_user_question uq " +
            "                WHERE uq.user_id = #{userId} AND uq.question_id = q.id) " +
            "ORDER BY RAND() LIMIT #{count}" +
            "</script>")
    List<Long> randomUnknownIds(@Param("userId") Long userId,
                                @Param("category") String category,
                                @Param("examType") String examType,
                                @Param("count") int count);

    /** 抽已答对的题 */
    @Select("<script>" +
            "SELECT q.id FROM t_question q " +
            "INNER JOIN t_user_question uq ON uq.question_id = q.id " +
            "WHERE q.deleted = 0 AND uq.user_id = #{userId} AND uq.is_correct = 1 " +
            "<if test='category != null and category != \"\"'>AND q.category = #{category}</if> " +
            "<if test='examType != null and examType != \"\"'>AND q.exam_type = #{examType}</if> " +
            "<if test='examSubType != null and examSubType != \"\"'>AND q.exam_sub_type = #{examSubType}</if> " +
            "ORDER BY RAND() LIMIT #{count}" +
            "</script>")
    List<Long> randomCorrectIds(@Param("userId") Long userId,
                                @Param("category") String category,
                                @Param("examType") String examType,
                                @Param("examSubType") String examSubType,
                                @Param("count") int count);

    /** 抽已答错的题 */
    @Select("<script>" +
            "SELECT q.id FROM t_question q " +
            "INNER JOIN t_user_question uq ON uq.question_id = q.id " +
            "WHERE q.deleted = 0 AND uq.user_id = #{userId} AND uq.is_correct = 0 " +
            "<if test='category != null and category != \"\"'>AND q.category = #{category}</if> " +
            "<if test='examType != null and examType != \"\"'>AND q.exam_type = #{examType}</if> " +
            "<if test='examSubType != null and examSubType != \"\"'>AND q.exam_sub_type = #{examSubType}</if> " +
            "ORDER BY RAND() LIMIT #{count}" +
            "</script>")
    List<Long> randomWrongIds(@Param("userId") Long userId,
                              @Param("category") String category,
                              @Param("examType") String examType,
                              @Param("examSubType") String examSubType,
                              @Param("count") int count);
}
