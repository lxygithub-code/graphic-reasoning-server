package com.lee.graphic_reasoning_server.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lee.graphic_reasoning_server.mapper.PracticeRecordMapper;
import com.lee.graphic_reasoning_server.mapper.QuestionMapper;
import com.lee.graphic_reasoning_server.mapper.UserMapper;
import com.lee.graphic_reasoning_server.po.PracticeRecord;
import com.lee.graphic_reasoning_server.po.User;
import com.lee.graphic_reasoning_server.service.StatService;
import com.lee.graphic_reasoning_server.vo.StatVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StatServiceImpl implements StatService {

    private final UserMapper userMapper;
    private final QuestionMapper questionMapper;
    private final PracticeRecordMapper practiceRecordMapper;

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
}