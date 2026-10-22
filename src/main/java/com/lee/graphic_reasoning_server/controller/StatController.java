package com.lee.graphic_reasoning_server.controller;

import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.common.R;
import com.lee.graphic_reasoning_server.service.StatService;
import com.lee.graphic_reasoning_server.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class StatController {

    private final StatService statService;

    @GetMapping
    public R<StatVO> overview() {
        return R.ok(statService.overview());
    }


    /** ★ 四大类题目数量分布 */
    @GetMapping("/category")
    public R<List<Map<String, Object>>> category() {
        return R.ok(statService.categoryDistribution());
    }

    /** ★ 活跃趋势：近 N 天活跃用户数 */
    @GetMapping("/active-trend")
    public R<ActiveTrendVO> activeTrend(@RequestParam(defaultValue = "7") Integer days) {
        return R.ok(statService.activeTrend(days));
    }

    /** ★ 今日活跃用户列表 */
    @GetMapping("/today-active-users")
    public R<PageVO<UserDailyActiveVO>> todayActiveUsers(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return R.ok(statService.todayActiveUsers(pageNum, pageSize));
    }

    /** ★ 可抽取题目趋势（按考试类型） */
    @GetMapping("/extractable-trend")
    public R<Map<String, Object>> extractableTrend(
            @RequestParam(defaultValue = "7") Integer days) {
        return R.ok(statService.extractableTrend(days));
    }

    /** ★ 今日活跃峰值时段 */
    @GetMapping("/peak-hour")
    public R<Map<String, Object>> peakHour() {
        return R.ok(statService.peakHour());
    }

    /** ★ 今日各时段活跃用户数 */
    @GetMapping("/today-hourly-active")
    public R<Map<String, Object>> todayHourlyActive() {
        return R.ok(statService.todayHourlyActive());
    }


    /** 用户活跃与请求统计 */
    @GetMapping("/engagement")
    public R<UserEngagementVO> engagement(@RequestParam(defaultValue = "7") Integer days) {
        return R.ok(statService.getEngagement(days));
    }

    /** 错题重灾区分析 */
    @GetMapping("/wrong-analysis")
    public R<WrongAnalysisVO> wrongAnalysis() {
        return R.ok(statService.getWrongAnalysis());
    }

    @GetMapping("/retention")
    public R<UserRetentionVO> retention(@RequestParam(defaultValue = "7") Integer days) {
        return R.ok(statService.getUserRetention(days));
    }

    /** 获取活跃分层用户明细 */
    @GetMapping("/retention-users")
    public R<List<Map<String, Object>>> retentionUsers(
            @RequestParam(defaultValue = "7") Integer days,
            @RequestParam(defaultValue = "multiple") String type) {
        return R.ok(statService.getRetentionUserList(days, type));
    }
}