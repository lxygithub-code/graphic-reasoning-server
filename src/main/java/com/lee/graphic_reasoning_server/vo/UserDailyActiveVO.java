package com.lee.graphic_reasoning_server.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserDailyActiveVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String nickname;
    private String avatarUrl;

    private LocalDate activeDate;
    private Integer requestCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime firstTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastTime;
}