package com.lee.graphic_reasoning_server.task;

import com.lee.graphic_reasoning_server.mapper.PracticeDetailMapper;
import com.lee.graphic_reasoning_server.mapper.QuestionMapper;
import com.lee.graphic_reasoning_server.po.Question;
import com.lee.graphic_reasoning_server.vo.QuestionDifficultyStatVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionDifficultyTask {

    private final PracticeDetailMapper detailMapper;
    private final QuestionMapper questionMapper;

    /** ★ 触发阈值：累计作答次数达到这个数才评估 */
    private static final int MIN_ANSWER_COUNT = 20;

    /**
     * 每天 0 点整执行
     * cron 格式：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void scheduledRefresh() {
        log.info("[难度刷新] 定时任务开始");
        try {
            Map<String, Object> result = doRefresh();
            log.info("[难度刷新] 完成：{}", result);
        } catch (Exception e) {
            log.error("[难度刷新] 执行失败", e);
        }
    }

    /**
     * 真正的刷新逻辑（抽出来方便手动触发 / 单测）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> doRefresh() {
        // 1. 统计：只返回累计作答 >= 20 次的题目
        List<QuestionDifficultyStatVO> stats =
                detailMapper.selectDifficultyStats(MIN_ANSWER_COUNT);

        int updated = 0;
        int skipped = 0;

        for (QuestionDifficultyStatVO s : stats) {
            Long total = s.getTotalCount();
            Long correct = s.getCorrectCount();
            if (total == null || total == 0) { skipped++; continue; }

            // 2. 计算正确率（百分比 0-100）
            double accuracy = correct * 100.0 / total;

            // 3. 按正确率映射难度星级
            int level = computeLevel(accuracy);

            // 4. 更新题目难度（仅更新 difficulty 字段）
            Question update = new Question();
            update.setId(s.getQuestionId());
            update.setDifficulty(level);
            int rows = questionMapper.updateById(update);
            if (rows > 0) updated++;
            else skipped++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("scanned", stats.size());
        result.put("updated", updated);
        result.put("skipped", skipped);
        result.put("minAnswerCount", MIN_ANSWER_COUNT);
        return result;
    }

    /**
     * 正确率 → 难度星级
     * ≤20%  → 5 星（最难）
     * ≤40%  → 4 星
     * ≤60%  → 3 星
     * ≤80%  → 2 星
     * >80%  → 1 星（最容易）
     */
    private int computeLevel(double accuracy) {
        if (accuracy <= 20) return 5;
        if (accuracy <= 40) return 4;
        if (accuracy <= 60) return 3;
        if (accuracy <= 80) return 2;
        return 1;
    }
}
