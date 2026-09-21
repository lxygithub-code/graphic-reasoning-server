package com.lee.graphic_reasoning_server.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lee.graphic_reasoning_server.mapper.*;
import com.lee.graphic_reasoning_server.po.Dict;
import com.lee.graphic_reasoning_server.po.PracticeRecord;
import com.lee.graphic_reasoning_server.po.Question;
import com.lee.graphic_reasoning_server.po.User;
import com.lee.graphic_reasoning_server.service.StatService;
import com.lee.graphic_reasoning_server.vo.StatVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatServiceImpl implements StatService {

    private final UserMapper userMapper;
    private final QuestionMapper questionMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final PracticeRecordMapper recordMapper;
    private final DictMapper dictMapper;


    @Override
    public StatVO overview() {
        StatVO vo = new StatVO();

        vo.setUserCount(userMapper.selectCount(null));
        vo.setQuestionCount(questionMapper.selectCount(null));
        vo.setPracticeCount(practiceRecordMapper.selectCount(null));

        // 平均正确率：取所有练习记录的平均值
        Double avg = practiceRecordMapper.selectAvgAccuracy();
        vo.setAvgAccuracy(avg == null ? 0.0 : Math.round(avg * 100) / 100.0);

        // 今日新增用户
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        vo.setTodayUserCount(userMapper.selectCount(
                new LambdaQueryWrapper<User>().ge(User::getCreateTime, todayStart)));

        // 今日练习次数
        vo.setTodayPracticeCount(practiceRecordMapper.selectCount(
                new LambdaQueryWrapper<PracticeRecord>().ge(PracticeRecord::getCreateTime, todayStart)));

        return vo;
    }

    @Override
    public Map<String, Object> growth(Integer days) {
        if (days == null || days <= 0 || days > 90) days = 7;

        List<String> dates = new ArrayList<>();
        List<Long> newUsers = new ArrayList<>();
        List<Long> newQuestions = new ArrayList<>();
        List<Long> newPractices = new ArrayList<>();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime start = day.atStartOfDay();
            LocalDateTime end = day.plusDays(1).atStartOfDay();

            dates.add(day.format(fmt));
            newUsers.add(userMapper.selectCount(
                    new LambdaQueryWrapper<User>().ge(User::getCreateTime, start).lt(User::getCreateTime, end)));
            newQuestions.add(questionMapper.selectCount(
                    new LambdaQueryWrapper<Question>().ge(Question::getCreateTime, start).lt(Question::getCreateTime, end)));
            newPractices.add(recordMapper.selectCount(
                    new LambdaQueryWrapper<PracticeRecord>().ge(PracticeRecord::getCreateTime, start).lt(PracticeRecord::getCreateTime, end)));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("newUsers", newUsers);
        result.put("newQuestions", newQuestions);
        result.put("newPractices", newPractices);
        return result;
    }

    @Override
    public Map<String, Object> active(Integer days) {
        if (days == null || days <= 0 || days > 90) days = 7;

        List<String> dates = new ArrayList<>();
        List<Long> activeUsers = new ArrayList<>();
        List<Long> practiceCount = new ArrayList<>();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime start = day.atStartOfDay();
            LocalDateTime end = day.plusDays(1).atStartOfDay();

            dates.add(day.format(fmt));
            // 活跃用户数：当天有练习记录的去重用户数
            Long users = recordMapper.selectCount(
                    new LambdaQueryWrapper<PracticeRecord>()
                            .select(PracticeRecord::getUserId)
                            .ge(PracticeRecord::getCreateTime, start)
                            .lt(PracticeRecord::getCreateTime, end)
                            .groupBy(PracticeRecord::getUserId)
                            .last("LIMIT 1"));
            // ★ 上面这条不好用，直接用 SQL 去重
            activeUsers.add(activeUsersByDay(start, end));
            practiceCount.add(recordMapper.selectCount(
                    new LambdaQueryWrapper<PracticeRecord>()
                            .ge(PracticeRecord::getCreateTime, start)
                            .lt(PracticeRecord::getCreateTime, end)));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("activeUsers", activeUsers);
        result.put("practiceCount", practiceCount);
        return result;
    }

    /** 去重统计当天活跃用户数 */
    private Long activeUsersByDay(LocalDateTime start, LocalDateTime end) {
        QueryWrapper<PracticeRecord> qw = new QueryWrapper<>();
        qw.select("COUNT(DISTINCT user_id) AS cnt")
                .ge("create_time", start)
                .lt("create_time", end);
        Map<String, Object> map = recordMapper.selectMaps(qw).get(0);
        Object cnt = map.get("cnt");
        return cnt == null ? 0L : Long.valueOf(cnt.toString());
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
            // 统计该一级分类下所有二级分类的题目总数
            List<Dict> l2List = dictMapper.selectList(
                    new LambdaQueryWrapper<Dict>()
                            .eq(Dict::getParentId, l1.getId())
                            .eq(Dict::getStatus, 1));
            List<String> labelList = new ArrayList<>();
            labelList.add(l1.getDictLabel());
            for (Dict l2 : l2List) labelList.add(l1.getDictLabel() + "/" + l2.getDictLabel());

            // 用 like 匹配 category 字段（category 存的是"一级/二级"）
            Long count = questionMapper.selectCount(
                    new LambdaQueryWrapper<Question>()
                            .and(w -> {
                                w.likeRight(Question::getCategory, l1.getDictLabel() + "/")
                                        .or()
                                        .eq(Question::getCategory, l1.getDictLabel());
                            }));

            Map<String, Object> item = new HashMap<>();
            item.put("name", l1.getDictLabel());
            item.put("value", count);
            result.add(item);
        }

        // 未分类
        Long uncategorized = questionMapper.selectCount(
                new LambdaQueryWrapper<Question>()
                        .and(w -> w.isNull(Question::getCategory).or().eq(Question::getCategory, "")));
        if (uncategorized > 0) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", "未分类");
            item.put("value", uncategorized);
            result.add(item);
        }

        return result;
    }
}