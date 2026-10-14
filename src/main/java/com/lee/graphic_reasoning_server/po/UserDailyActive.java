package com.lee.graphic_reasoning_server.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_user_daily_active")
public class UserDailyActive implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate activeDate;

    /** 当日请求次数 */
    private Integer requestCount;

    private LocalDateTime firstTime;

    private LocalDateTime lastTime;
}
