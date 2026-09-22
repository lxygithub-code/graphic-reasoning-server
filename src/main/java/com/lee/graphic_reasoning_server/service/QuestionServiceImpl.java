package com.lee.graphic_reasoning_server.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lee.graphic_reasoning_server.common.BizException;
import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.common.UserContext;
import com.lee.graphic_reasoning_server.dto.QuestionQueryDTO;
import com.lee.graphic_reasoning_server.dto.QuestionRandomDTO;
import com.lee.graphic_reasoning_server.dto.QuestionSaveDTO;
import com.lee.graphic_reasoning_server.mapper.QuestionAnalysisMapper;
import com.lee.graphic_reasoning_server.mapper.QuestionMapper;
import com.lee.graphic_reasoning_server.mapper.UserQuestionMapper;
import com.lee.graphic_reasoning_server.po.Question;
import com.lee.graphic_reasoning_server.po.QuestionAnalysis;
import com.lee.graphic_reasoning_server.vo.ExamTypeCountVO;
import com.lee.graphic_reasoning_server.vo.QuestionDetailVO;
import com.lee.graphic_reasoning_server.vo.QuestionPracticeVO;
import com.lee.graphic_reasoning_server.vo.QuestionSourceStatVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionMapper questionMapper;
    private final UserQuestionMapper userQuestionMapper;

    @Override
    public List<QuestionPracticeVO> randomPractice(QuestionRandomDTO dto) {
        Long userId = UserContext.get();
        if (userId == null) throw new BizException("未登录");

        int count = dto.getCount() == null ? 10 : dto.getCount();
        if (count <= 0 || count > 50) count = 10;

        // 1. 校验权重：三者之和必须 100，否则用默认值
        int wUnknown = dto.getWeightUnknown() == null ? 60 : dto.getWeightUnknown();
        int wCorrect = dto.getWeightCorrect() == null ? 20 : dto.getWeightCorrect();
        int wWrong   = dto.getWeightWrong()   == null ? 20 : dto.getWeightWrong();
        if (wUnknown + wCorrect + wWrong != 100) {
            wUnknown = 60; wCorrect = 20; wWrong = 20;
        }

        // 2. 按权重分配名额
        int targetUnknown = Math.max(0, Math.round(count * wUnknown / 100f));
        int targetCorrect = Math.max(0, Math.round(count * wCorrect / 100f));
        int targetWrong   = count - targetUnknown - targetCorrect;
        if (targetWrong < 0) {
            // 修一下，保证三者加起来是 count
            targetWrong = 0;
            targetUnknown = count - targetCorrect;
        }

        // 3. 分别抽
        List<Long> ids = new ArrayList<>();
        // 权重分配后调用：
        List<Long> unknownIds = questionMapper.randomUnknownIds(
                userId, dto.getCategory(), dto.getExamType(), dto.getExamSubType(), targetUnknown);

        List<Long> correctIds = userQuestionMapper.randomCorrectIds(
                userId, dto.getCategory(), dto.getExamType(), dto.getExamSubType(), targetCorrect);

        List<Long> wrongIds = userQuestionMapper.randomWrongIds(
                userId, dto.getCategory(), dto.getExamType(), dto.getExamSubType(), targetWrong);

        ids.addAll(unknownIds);
        ids.addAll(correctIds);
        ids.addAll(wrongIds);
        /**
         * 4. 抽不满，从其它类补
         *
         * 先抽：未答 6 + 已对 2 + 已错 1 = 9 题
         * 缺 1 题 → 从未答题池补
         * 未答也不够 → 从已答对池补
         * 已答对也不够 → 从已答错池补
         * 全不够 → 返回实际数量（可能少于 count）
         */
        if (ids.size() < count) {
            int lack = count - ids.size();
            // 先补未答题（数量最多）
            List<Long> moreUnknown = questionMapper.randomUnknownIds(userId, dto.getCategory(), dto.getExamType(), dto.getExamSubType(), lack + ids.size());
            for (Long id : moreUnknown) {
                if (!ids.contains(id)) {
                    ids.add(id);
                    if (ids.size() >= count) break;
                }
            }
        }
        if (ids.size() < count) {
            int lack = count - ids.size();
            List<Long> moreCorrect = userQuestionMapper.randomCorrectIds(userId, dto.getCategory(), dto.getExamType(), dto.getExamSubType(), lack + ids.size());
            for (Long id : moreCorrect) {
                if (!ids.contains(id)) {
                    ids.add(id);
                    if (ids.size() >= count) break;
                }
            }
        }
        if (ids.size() < count) {
            int lack = count - ids.size();
            List<Long> moreWrong = userQuestionMapper.randomWrongIds(userId, dto.getCategory(), dto.getExamType(), dto.getExamSubType(), lack + ids.size());
            for (Long id : moreWrong) {
                if (!ids.contains(id)) {
                    ids.add(id);
                    if (ids.size() >= count) break;
                }
            }
        }

        // 5. 打乱顺序
        Collections.shuffle(ids);

        // 6. 截断到 count
        if (ids.size() > count) {
            ids = ids.subList(0, count);
        }

        if (ids.isEmpty()) return Collections.emptyList();

        // 7. 查询题目（selectBatchIds 会走 autoResultMap）
        List<Question> list = questionMapper.selectByIds(ids);
        Map<Long, Question> map = list.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        return ids.stream()
                .map(map::get)
                .filter(Objects::nonNull)
                .map(q -> {
                    QuestionPracticeVO vo = new QuestionPracticeVO();
                    BeanUtil.copyProperties(q, vo);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public QuestionDetailVO detail(Long id) {
        Question q = questionMapper.selectById(id);
        if (q == null) throw new BizException("题目不存在");
        QuestionDetailVO vo = new QuestionDetailVO();
        BeanUtil.copyProperties(q, vo);
        vo.setAnalyses(listAnalyses(id));
        return vo;
    }

    @Override
    public PageVO<QuestionDetailVO> page(QuestionQueryDTO dto) {
        LambdaQueryWrapper<Question> qw = new LambdaQueryWrapper<Question>()
                .eq(StrUtil.isNotBlank(dto.getCategory()), Question::getCategory, dto.getCategory())
                .eq(StrUtil.isNotBlank(dto.getExamType()), Question::getExamType, dto.getExamType())
                .eq(StrUtil.isNotBlank(dto.getExamSubType()), Question::getExamSubType, dto.getExamSubType())
                .eq(StrUtil.isNotBlank(dto.getSource()), Question::getSource, dto.getSource())
                .eq(dto.getDifficulty() != null, Question::getDifficulty, dto.getDifficulty())
                .like(StrUtil.isNotBlank(dto.getKeyword()), Question::getContent, dto.getKeyword())
                .orderByDesc(Question::getId);

        Page<Question> page = questionMapper.selectPage(
                new Page<>(dto.getPageNum(), dto.getPageSize()), qw);

        List<QuestionDetailVO> records = page.getRecords().stream().map(q -> {
            QuestionDetailVO vo = new QuestionDetailVO();
            BeanUtil.copyProperties(q, vo);
            return vo;
        }).collect(Collectors.toList());

        return PageVO.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    @Transactional
    public Long save(QuestionSaveDTO dto) {
        validate(dto);
        Question q = new Question();
        BeanUtil.copyProperties(dto, q, "id", "analyses");
        questionMapper.insert(q);

        saveAnalyses(q.getId(), dto.getAnalyses());   // ★
        return q.getId();
    }

    /** 覆盖式写入：先删后插 */
    private void saveAnalyses(Long questionId, List<QuestionSaveDTO.AnalysisItem> analyses) {
        // 先清空
        analysisMapper.delete(new LambdaQueryWrapper<QuestionAnalysis>()
                .eq(QuestionAnalysis::getQuestionId, questionId));

        if (analyses == null || analyses.isEmpty()) return;

        int sort = 0;
        Set<String> usedPlatforms = new HashSet<>();
        for (QuestionSaveDTO.AnalysisItem item : analyses) {
            if (StrUtil.isBlank(item.getPlatform()) || StrUtil.isBlank(item.getContent())) continue;
            if (usedPlatforms.contains(item.getPlatform())) continue;   // 同平台只留一条
            usedPlatforms.add(item.getPlatform());

            QuestionAnalysis a = new QuestionAnalysis();
            a.setQuestionId(questionId);
            a.setPlatform(item.getPlatform());
            a.setType(StrUtil.blankToDefault(item.getType(), "text"));
            a.setContent(item.getContent().trim());
            a.setSort(sort++);
            analysisMapper.insert(a);
        }
    }

    @Override
    @Transactional
    public void update(QuestionSaveDTO dto) {
        if (dto.getId() == null) throw new BizException("id 不能为空");
        validate(dto);
        Question q = new Question();
        BeanUtil.copyProperties(dto, q, "analyses");
        questionMapper.updateById(q);

        saveAnalyses(dto.getId(), dto.getAnalyses());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        questionMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        questionMapper.deleteByIds(ids);
    }

    private void validate(QuestionSaveDTO dto) {
        boolean exists = dto.getOptions().stream()
                .anyMatch(o -> o.getKey() != null
                        && o.getKey().equalsIgnoreCase(dto.getCorrectOption()));
        if (!exists) throw new BizException("正确答案必须存在于选项中");

        for (Question.Option opt : dto.getOptions()) {
            if (StrUtil.isBlank(opt.getKey())) {
                throw new BizException("选项 key 不能为空");
            }
            // type 缺省按 text 处理，防止前端漏传
            if (StrUtil.isBlank(opt.getType())) {
                opt.setType("text");
            }
            if (!"text".equals(opt.getType()) && !"image".equals(opt.getType())) {
                throw new BizException("选项 " + opt.getKey() + " 的类型只能是 text 或 image");
            }
            if (StrUtil.isBlank(opt.getValue())) {
                throw new BizException("选项 " + opt.getKey() + " 的内容不能为空");
            }
        }
    }

    @Override
    public List<QuestionSourceStatVO> sourceStats(String examType, String examSubType, String keyword) {
        return questionMapper.selectSourceStats(examType, examSubType, keyword);
    }

    @Override
    public List<QuestionDetailVO> listBySource(String source) {
        LambdaQueryWrapper<Question> qw = new LambdaQueryWrapper<Question>()
                .orderByAsc(Question::getId);

        if (StrUtil.isBlank(source)) {
            // 未分类：source 为 null 或 ''
            qw.and(w -> w.isNull(Question::getSource).or().eq(Question::getSource, ""));
        } else {
            qw.eq(Question::getSource, source);
        }

        List<Question> list = questionMapper.selectList(qw);
        return list.stream().map(q -> {
            QuestionDetailVO vo = new QuestionDetailVO();
            BeanUtil.copyProperties(q, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    private final QuestionAnalysisMapper analysisMapper;

    @Override
    public List<QuestionDetailVO.AnalysisVO> listAnalyses(Long questionId) {
        List<QuestionAnalysis> list = analysisMapper.selectList(
                new LambdaQueryWrapper<QuestionAnalysis>()
                        .eq(QuestionAnalysis::getQuestionId, questionId)
                        .orderByAsc(QuestionAnalysis::getSort)
                        .orderByAsc(QuestionAnalysis::getId));
        return list.stream().map(a -> {
            QuestionDetailVO.AnalysisVO vo = new QuestionDetailVO.AnalysisVO();
            vo.setPlatform(a.getPlatform());
            vo.setType(StrUtil.blankToDefault(a.getType(), "text"));
            vo.setContent(a.getContent());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ExamTypeCountVO> countByExamType() {
        return questionMapper.selectCountByExamType();
    }
}
