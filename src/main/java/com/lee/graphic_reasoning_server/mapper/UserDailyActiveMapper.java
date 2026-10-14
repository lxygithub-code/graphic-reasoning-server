package com.lee.graphic_reasoning_server.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lee.graphic_reasoning_server.po.UserDailyActive;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

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
}