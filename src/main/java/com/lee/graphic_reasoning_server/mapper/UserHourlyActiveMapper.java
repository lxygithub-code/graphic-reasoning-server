package com.lee.graphic_reasoning_server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lee.graphic_reasoning_server.po.UserHourlyActive;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserHourlyActiveMapper extends BaseMapper<UserHourlyActive> {

    /**
     * 幂等插入（用户在某天某小时只记一次）
     */
    @Insert("""
                INSERT IGNORE INTO t_user_hourly_active
                    (user_id, active_date, active_hour, create_time)
                VALUES
                    (#{userId}, #{date}, #{hour}, NOW())
            """)
    void insertIgnore(@Param("userId") Long userId,
                      @Param("date") LocalDate date,
                      @Param("hour") Integer hour);

    /**
     * 查询某天活跃用户最多的时段
     */
    @Select("""
                SELECT active_hour AS hour,
                       COUNT(DISTINCT user_id) AS cnt
                FROM t_user_hourly_active
                WHERE active_date = #{date}
                GROUP BY active_hour
                ORDER BY cnt DESC, active_hour ASC
                LIMIT 1
            """)
    Map<String, Object> selectPeakHour(@Param("date") LocalDate date);

    /**
     * 查询某天各小时的活跃用户数
     */
    @Select("""
                SELECT active_hour AS hour,
                       COUNT(DISTINCT user_id) AS cnt
                FROM t_user_hourly_active
                WHERE active_date = #{date}
                GROUP BY active_hour
                ORDER BY active_hour
            """)
    List<Map<String, Object>> selectHourlyCount(@Param("date") LocalDate date);
}
