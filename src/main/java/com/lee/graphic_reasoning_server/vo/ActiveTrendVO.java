package com.lee.graphic_reasoning_server.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class ActiveTrendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 日期标签（MM-dd） */
    private List<String> dates = new ArrayList<>();

    /** 每日活跃用户数 */
    private List<Long> counts = new ArrayList<>();

    /** 今日活跃数 */
    private Long todayCount = 0L;

    /** 昨日活跃数 */
    private Long yesterdayCount = 0L;
}