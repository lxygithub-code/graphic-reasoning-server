package com.lee.graphic_reasoning_server.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lee.graphic_reasoning_server.po.UserDailyActive;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserDailyActiveMapper extends BaseMapper<UserDailyActive> {

    /**
     * UPSERT：存在则计数+1、更新 last_time；不存在则插入
     */
    @Insert("""
                INSERT INTO t_user_daily_active
                    (user_id, active_date, request_count, first_time, last_time)
                VALUES
                    (#{userId}, #{date}, 1, NOW(), NOW())
                ON DUPLICATE KEY UPDATE
                    request_count = request_count + 1,
                    last_time = NOW()
            """)
    void upsert(@Param("userId") Long userId, @Param("date") LocalDate date);


    /**
     * 统计某段时间内的总请求次数
     */
    @Select("SELECT SUM(request_count) FROM t_user_daily_active WHERE active_date >= #{startDate}")
    Long selectTotalRequests(@Param("startDate") LocalDate startDate);

    /**
     * 统计某段时间内的每日总请求次数
     */
    @Select("SELECT active_date AS date, SUM(request_count) AS count FROM t_user_daily_active WHERE active_date >= #{startDate} GROUP BY active_date ORDER BY active_date")
    List<Map<String, Object>> selectDailyRequests(@Param("startDate") LocalDate startDate);

    /**
     * 统计指定时间段内，活跃用户的天数分层
     */
    @Select("""
                SELECT 
                  SUM(CASE WHEN cnt = 1 THEN 1 ELSE 0 END) AS onceCount,
                  SUM(CASE WHEN cnt >= 2 THEN 1 ELSE 0 END) AS multipleCount
                FROM (
                  SELECT user_id, COUNT(*) as cnt
                  FROM t_user_daily_active
                  WHERE active_date >= #{startDate}
                  GROUP BY user_id
                ) t
            """)
    Map<String, Object> selectRetentionStats(@Param("startDate") LocalDate startDate);

    /** 获取指定活跃分层（一次/多次）的用户明细列表 */
    @Select("<script>" +
            "SELECT a.user_id AS userId, COUNT(a.active_date) AS activeDays, " +
            "u.nickname AS nickname, u.avatar_url AS avatarUrl " +
            "FROM t_user_daily_active a " +
            "LEFT JOIN t_user u ON a.user_id = u.id " +
            "WHERE a.active_date &gt;= #{startDate} " +
            "GROUP BY a.user_id, u.nickname, u.avatar_url " +
            "<choose>" +
            "  <when test='type == \"once\"'> HAVING COUNT(a.active_date) = 1 </when>" +
            "  <otherwise> HAVING COUNT(a.active_date) &gt;= 2 </otherwise>" +
            "</choose>" +
            "ORDER BY activeDays DESC " +
            "</script>")
    List<Map<String, Object>> selectRetentionUserList(@Param("startDate") LocalDate startDate, @Param("type") String type);
}