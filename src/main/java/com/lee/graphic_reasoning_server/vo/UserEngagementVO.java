package com.lee.graphic_reasoning_server.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserEngagementVO {
    private Long totalActiveDays;     // 累计活跃天数（人次）
    private Long totalRequests;       // 累计请求次数
    private List<String> dates;       // 日期
    private List<Long> dailyRequests; // 每日请求次数
}