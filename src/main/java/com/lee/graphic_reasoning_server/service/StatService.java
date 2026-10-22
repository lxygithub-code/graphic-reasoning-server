package com.lee.graphic_reasoning_server.service;

import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.vo.*;

import java.util.List;
import java.util.Map;

public interface StatService {
    StatVO overview();

    List<Map<String, Object>> categoryDistribution();

    /** 活跃趋势：近 N 天活跃用户数 */
    ActiveTrendVO activeTrend(Integer days);

    /** 今日活跃用户列表（分页） */
    PageVO<UserDailyActiveVO> todayActiveUsers(Integer pageNum, Integer pageSize);

    Map<String, Object> extractableTrend(Integer days);

    /** 今日活跃峰值时段 */
    Map<String, Object> peakHour();

    /** 今日各时段活跃用户数 */
    Map<String, Object> todayHourlyActive();

    UserEngagementVO getEngagement(Integer days);

    WrongAnalysisVO getWrongAnalysis();

    UserRetentionVO getUserRetention(Integer days);

    List<Map<String, Object>> getRetentionUserList(Integer days, String type);
}
