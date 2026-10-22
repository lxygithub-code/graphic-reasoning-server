package com.lee.graphic_reasoning_server.service;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.mapper.*;
import com.lee.graphic_reasoning_server.po.*;
import com.lee.graphic_reasoning_server.util.DictTranslator;
import com.lee.graphic_reasoning_server.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatServiceImpl implements StatService {

    private final UserMapper userMapper;
    private final QuestionMapper questionMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final DictMapper dictMapper;
    private final UserDailyActiveMapper dailyActiveMapper;
    private final UserHourlyActiveMapper hourlyActiveMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final DictTranslator dictTranslator;

    @Override
    public StatVO overview() {
        StatVO vo = new StatVO();

        vo.setUserCount(userMapper.selectCount(null));
        vo.setQuestionCount(questionMapper.selectCount(null));

        // 今日新增用户
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        vo.setTodayUserCount(userMapper.selectCount(
                new LambdaQueryWrapper<User>().ge(User::getCreateTime, todayStart)));

        // 今日练习次数
        vo.setTodayPracticeCount(practiceRecordMapper.selectCount(
                new LambdaQueryWrapper<PracticeRecord>().ge(PracticeRecord::getCreateTime, todayStart)));

        // 可抽取题目数
        vo.setExtractableCount(questionMapper.selectCount(
                new LambdaQueryWrapper<Question>().eq(Question::getCanExtract, 1)));
        return vo;
    }

    @Override
    public List<Map<String, Object>> categoryDistribution() {
        // 从 t_dict 拿一级分类（题目类型）
        List<Dict> l1List = dictMapper.selectList(
                new LambdaQueryWrapper<Dict>()
                        .eq(Dict::getDictType, "question_category")
                        .eq(Dict::getLevel, 1)
                        .eq(Dict::getStatus, 1));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Dict l1 : l1List) {
            // ★ 用 dictValue 匹配，不用 dictLabel
            String l1Value = l1.getDictValue();

            Long count = questionMapper.selectCount(
                    new LambdaQueryWrapper<Question>()
                            .and(w -> w
                                    // 自身等于一级 value
                                    .eq(Question::getCategory, l1Value)
                                    // 或者以 "一级 value/" 开头（含所有子类）
                                    .or()
                                    .likeRight(Question::getCategory, l1Value + "/"))
                            .and(w -> w.eq(Question::getCanExtract, 1)));

            Map<String, Object> item = new HashMap<>();
            item.put("name", l1.getDictLabel());   // 展示用 label
            item.put("value", count);
            result.add(item);
        }

        // 未分类
        Long uncategorized = questionMapper.selectCount(
                new LambdaQueryWrapper<Question>()
                        .and(w -> w.isNull(Question::getCategory).or().eq(Question::getCategory, ""))
                        .and(w -> w.eq(Question::getCanExtract, 1)));
        if (uncategorized > 0) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", "未分类");
            item.put("value", uncategorized);
            result.add(item);
        }

        return result;
    }

    @Override
    public ActiveTrendVO activeTrend(Integer days) {
        if (days == null || days <= 0 || days > 90) days = 7;

        ActiveTrendVO vo = new ActiveTrendVO();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            Long count = dailyActiveMapper.selectCount(
                    new LambdaQueryWrapper<UserDailyActive>()
                            .eq(UserDailyActive::getActiveDate, d));

            vo.getDates().add(d.format(fmt));
            vo.getCounts().add(count == null ? 0L : count);
        }

        // 今日/昨日
        if (!vo.getCounts().isEmpty()) {
            vo.setTodayCount(vo.getCounts().get(vo.getCounts().size() - 1));
            if (vo.getCounts().size() >= 2) {
                vo.setYesterdayCount(vo.getCounts().get(vo.getCounts().size() - 2));
            }
        }
        return vo;
    }

    @Override
    public PageVO<UserDailyActiveVO> todayActiveUsers(Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 20;

        Page<UserDailyActive> page = dailyActiveMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<UserDailyActive>()
                        .eq(UserDailyActive::getActiveDate, LocalDate.now())
                        .orderByDesc(UserDailyActive::getLastTime));

        List<UserDailyActiveVO> records = new ArrayList<>();
        if (!page.getRecords().isEmpty()) {
            List<Long> userIds = page.getRecords().stream()
                    .map(UserDailyActive::getUserId)
                    .distinct()
                    .collect(Collectors.toList());

            Map<Long, User> userMap = userMapper.selectByIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));

            for (UserDailyActive a : page.getRecords()) {
                UserDailyActiveVO vo = new UserDailyActiveVO();
                vo.setUserId(a.getUserId());
                vo.setActiveDate(a.getActiveDate());
                vo.setRequestCount(a.getRequestCount());
                vo.setFirstTime(a.getFirstTime());
                vo.setLastTime(a.getLastTime());

                User u = userMap.get(a.getUserId());
                if (u != null) {
                    vo.setNickname(StrUtil.blankToDefault(u.getNickname(), "匿名"));
                    vo.setAvatarUrl(u.getAvatarUrl());
                } else {
                    vo.setNickname("匿名");
                }
                records.add(vo);
            }
        }

        return PageVO.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public Map<String, Object> extractableTrend(Integer days) {
        if (days == null || days <= 0 || days > 90) {
            days = 7;
        }

        List<String> dates = new ArrayList<>();
        List<Long> guokao = new ArrayList<>();
        List<Long> shengkao = new ArrayList<>();
        List<Long> shiye = new ArrayList<>();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            // 截止到当天 23:59:59
            LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();

            dates.add(day.format(fmt));
            guokao.add(countExtractableUpTo("guokao", dayEnd));
            shengkao.add(countExtractableUpTo("shengkao", dayEnd));
            shiye.add(countExtractableUpTo("shiye", dayEnd));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("guokao", guokao);
        result.put("shengkao", shengkao);
        result.put("shiye", shiye);
        return result;
    }

    /**
     * 统计截止到某个时间点的可抽取题目总数（累积）
     */
    private Long countExtractableUpTo(String examType, LocalDateTime upTo) {
        Long c = questionMapper.selectCount(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getExamType, examType)
                        .eq(Question::getCanExtract, 1)
                        .lt(Question::getCreateTime, upTo));
        return c == null ? 0L : c;
    }


    @Override
    public Map<String, Object> peakHour() {
        Map<String, Object> result = new HashMap<>();
        LocalDate today = LocalDate.now();

        Map<String, Object> peak = hourlyActiveMapper.selectPeakHour(today);
        if (peak == null || peak.get("hour") == null) {
            result.put("hour", null);
            result.put("count", 0L);
            return result;
        }

        Object hourObj = peak.get("hour");
        Object cntObj = peak.get("cnt");
        int hour = ((Number) hourObj).intValue();
        long count = ((Number) cntObj).longValue();

        result.put("hour", hour);
        result.put("count", count);
        return result;
    }

    @Override
    public Map<String, Object> todayHourlyActive() {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> rows = hourlyActiveMapper.selectHourlyCount(today);

        // 24 个小时，缺失补 0
        long[] counts = new long[24];
        for (Map<String, Object> row : rows) {
            if (row.get("hour") == null || row.get("cnt") == null) continue;
            int h = ((Number) row.get("hour")).intValue();
            long c = ((Number) row.get("cnt")).longValue();
            if (h >= 0 && h < 24) counts[h] = c;
        }

        List<String> hours = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            hours.add(String.format("%02d:00", i));
            values.add(counts[i]);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("hours", hours);
        result.put("counts", values);
        return result;
    }

    @Override
    public UserEngagementVO getEngagement(Integer days) {
        if (days == null || days <= 0) days = 7;
        LocalDate startDate = LocalDate.now().minusDays(days - 1);

        UserEngagementVO vo = new UserEngagementVO();

        // 1. 累计请求次数
        Long totalReq = dailyActiveMapper.selectTotalRequests(startDate);
        vo.setTotalRequests(totalReq == null ? 0L : totalReq);

        // 2. 每日请求趋势
        List<Map<String, Object>> dailyMap = dailyActiveMapper.selectDailyRequests(startDate);
        List<String> dates = new ArrayList<>();
        List<Long> reqs = new ArrayList<>();

        Map<String, Long> map = new HashMap<>();
        for (Map<String, Object> row : dailyMap) {
            // row.get("date") 可能是 java.sql.Date，需要格式化
            String d = row.get("date").toString().substring(5); // 截取 MM-dd
            Long c = ((Number) row.get("count")).longValue();
            map.put(d, c);
        }

        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        long totalActive = 0;
        for (int i = days - 1; i >= 0; i--) {
            String d = today.minusDays(i).format(fmt);
            dates.add(d);
            Long c = map.getOrDefault(d, 0L);
            reqs.add(c);
            totalActive += c;
        }
        vo.setDates(dates);
        vo.setDailyRequests(reqs);
        vo.setTotalActiveDays(totalActive); // 这里用总请求量代替活跃人次，更直观

        return vo;
    }

    @Override
    public WrongAnalysisVO getWrongAnalysis() {
        WrongAnalysisVO vo = new WrongAnalysisVO();

        // 1. 错题类型分布（一级）
        List<Map<String, Object>> catRows = wrongQuestionMapper.selectWrongCategoryStats();
        List<Map<String, Object>> catStats = new ArrayList<>();
        for (Map<String, Object> row : catRows) {
            String catValue = (String) row.get("category");
            Long count = ((Number) row.get("totalWrong")).longValue();

            // 直接翻译一级 value 即可，不用 split
            String catLabel = dictTranslator.valuePathToLabelPath(catValue);
            if (StrUtil.isBlank(catLabel) || "null".equals(catLabel)) {
                catLabel = "未分类";
            }

            Map<String, Object> item = new HashMap<>();
            item.put("name", catLabel);
            item.put("value", count);
            catStats.add(item);
        }
        vo.setCategoryStats(catStats);

        // 2. 错题 TOP 10
        List<Map<String, Object>> topRows = wrongQuestionMapper.selectTopWrongQuestions();
        List<Map<String, Object>> topList = new ArrayList<>();
        for (Map<String, Object> row : topRows) {
            String catValue = (String) row.get("category");
            String catLabel = dictTranslator.valuePathToLabelPath(catValue);

            Map<String, Object> item = new HashMap<>();
            item.put("questionId", row.get("questionId"));
            // 题干截断，防止过长
            String content = (String) row.get("content");
            item.put("content", StrUtil.maxLength(content, 40));
            item.put("categoryLabel", catLabel);
            item.put("wrongCount", row.get("wrongCount"));
            topList.add(item);
        }
        vo.setTopWrongQuestions(topList);

        return vo;
    }

    @Override
    public UserRetentionVO getUserRetention(Integer days) {
        if (days == null || days <= 0) days = 7;
        LocalDate startDate = LocalDate.now().minusDays(days - 1);

        Map<String, Object> res = dailyActiveMapper.selectRetentionStats(startDate);
        UserRetentionVO vo = new UserRetentionVO();
        if (res != null) {
            if (res.get("onceCount") != null) vo.setOnceCount(((Number)res.get("onceCount")).longValue());
            if (res.get("multipleCount") != null) vo.setMultipleCount(((Number)res.get("multipleCount")).longValue());
        }
        return vo;
    }

    @Override
    public List<Map<String, Object>> getRetentionUserList(Integer days, String type) {
        if (days == null || days <= 0) days = 7;
        LocalDate startDate = LocalDate.now().minusDays(days - 1);
        return dailyActiveMapper.selectRetentionUserList(startDate, type);
    }
}