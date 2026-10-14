package com.lee.graphic_reasoning_server.vo;

import lombok.Data;

@Data
public class StatVO {
    private Long userCount;         // 用户总数
    private Long questionCount;     // 题目总数
    private Long todayUserCount;    // 今日新增用户
    private Long todayPracticeCount;// 今日练习次数
    /** 可抽取题目数 */
    private Long extractableCount;
}
