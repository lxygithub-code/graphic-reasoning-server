package com.lee.graphic_reasoning_server.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import com.lee.graphic_reasoning_server.util.DictTranslator;
import com.lee.graphic_reasoning_server.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionServiceImpl implements QuestionService {

    private final QuestionMapper questionMapper;
    private final UserQuestionMapper userQuestionMapper;
    private final QuestionAnalysisMapper analysisMapper;
    private final DictService dictService;
    private final DictTranslator dictTranslator;


    /** ★ custom / 空 → 视为不过滤 */
    private String normalizeExamType(String examType) {
        if (StrUtil.isBlank(examType)) {
            return null;
        }
        if ("custom".equalsIgnoreCase(examType)) {
            return null;
        }
        return examType;
    }

    @Override
    public List<QuestionPracticeVO> randomPractice(QuestionRandomDTO dto) {
        Long userId = UserContext.get();

        int count = dto.getCount() == null ? 10 : dto.getCount();
        if (count <= 0 || count > 50) {
            count = 10;
        }

        String examType = normalizeExamType(dto.getExamType());

        //游客模式：不区分权重，简单随机抽题
        if (userId == null) {
            log.warn("未登录,Guest mode: random practice");
            return randomForGuest(dto, count);
        }


        int wUnknown = dto.getWeightUnknown() == null ? 60 : dto.getWeightUnknown();
        int wCorrect = dto.getWeightCorrect() == null ? 20 : dto.getWeightCorrect();
        int wWrong = dto.getWeightWrong() == null ? 20 : dto.getWeightWrong();
        if (wUnknown + wCorrect + wWrong != 100) {
            wUnknown = 60;
            wCorrect = 20;
            wWrong = 20;
        }

        int targetUnknown = Math.max(0, Math.round(count * wUnknown / 100f));
        int targetCorrect = Math.max(0, Math.round(count * wCorrect / 100f));
        int targetWrong = count - targetUnknown - targetCorrect;
        if (targetWrong < 0) {
            targetWrong = 0;
            targetUnknown = count - targetCorrect;
        }

        List<Long> ids = new ArrayList<>();
        List<Long> unknownIds = questionMapper.randomUnknownIds(
                userId, dto.getCategory(), examType, dto.getExamSubType(), targetUnknown);
        List<Long> correctIds = userQuestionMapper.randomCorrectIds(
                userId, dto.getCategory(), examType, dto.getExamSubType(), targetCorrect);
        List<Long> wrongIds = userQuestionMapper.randomWrongIds(
                userId, dto.getCategory(), examType, dto.getExamSubType(), targetWrong);

        ids.addAll(unknownIds);
        ids.addAll(correctIds);
        ids.addAll(wrongIds);

        if (ids.size() < count) {
            int lack = count - ids.size();
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

        Collections.shuffle(ids);
        if (ids.size() > count) {
            ids = ids.subList(0, count);
        }
        if (ids.isEmpty()) return Collections.emptyList();

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

    /**
     * ★ 游客随机抽题：不区分权重、不查历史
     */
    private List<QuestionPracticeVO> randomForGuest(QuestionRandomDTO dto, int count) {
        List<Long> ids = questionMapper.randomPureIds(
                dto.getCategory(), dto.getExamType(), dto.getExamSubType(), count);
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<Question> list = questionMapper.selectByIds(ids);
        Map<Long, Question> map = list.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        // 保持 ids 顺序（SQL 里已经 RAND() 了）
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
        if (q == null) {
            throw new BizException("题目不存在");
        }
        QuestionDetailVO vo = new QuestionDetailVO();
        BeanUtil.copyProperties(q, vo);
        vo.setAnalyses(listAnalyses(id));
        vo.setCategoryLabel(dictTranslator.valuePathToLabelPath(vo.getCategory()));
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
        if (CollectionUtil.isNotEmpty(records)) {
            List<String> cats = records.stream()
                    .map(QuestionDetailVO::getCategory)
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .collect(Collectors.toList());
            Map<String, String> catMap = dictTranslator.batchValueToLabelPath(cats);
            records.forEach(vo -> vo.setCategoryLabel(
                    catMap.getOrDefault(vo.getCategory(), vo.getCategory())));
        }
        return PageVO.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    @Transactional
    public Long save(QuestionSaveDTO dto) {
        validate(dto);
        Question q = new Question();
        BeanUtil.copyProperties(dto, q, "id", "analyses");
        // 新增时若前端未传 canExtract，默认 1（可抽取）
        if (q.getCanExtract() == null) q.setCanExtract(1);
        questionMapper.insert(q);

        saveAnalyses(q.getId(), dto.getAnalyses());
        return q.getId();
    }

    /**
     * 覆盖式写入：先删后插
     */
    private void saveAnalyses(Long questionId, List<QuestionSaveDTO.AnalysisItem> analyses) {
        analysisMapper.delete(new LambdaQueryWrapper<QuestionAnalysis>()
                .eq(QuestionAnalysis::getQuestionId, questionId));

        if (analyses == null || analyses.isEmpty()) return;

        int sort = 0;
        Set<String> usedPlatforms = new HashSet<>();
        for (QuestionSaveDTO.AnalysisItem item : analyses) {
            if (StrUtil.isBlank(item.getPlatform()) || StrUtil.isBlank(item.getContent())) continue;
            if (usedPlatforms.contains(item.getPlatform())) continue;
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
            qw.and(w -> w.isNull(Question::getSource).or().eq(Question::getSource, ""));
        } else {
            qw.eq(Question::getSource, source);
        }

        List<Question> list = questionMapper.selectList(qw);
        List<QuestionDetailVO> records = list.stream().map(q -> {
            QuestionDetailVO vo = new QuestionDetailVO();
            BeanUtil.copyProperties(q, vo); // canExtract 会自动拷贝
            return vo;
        }).collect(Collectors.toList());
        if (CollectionUtil.isNotEmpty(records)) {
            List<String> cats = records.stream()
                    .map(QuestionDetailVO::getCategory)
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .collect(Collectors.toList());
            Map<String, String> catMap = dictTranslator.batchValueToLabelPath(cats);
            records.forEach(vo -> vo.setCategoryLabel(
                    catMap.getOrDefault(vo.getCategory(), vo.getCategory())));
        }
        return records;
    }

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

    // ==================== ★ 新增：可抽取控制 ====================

    @Override
    @Transactional
    public int updateSourceExtractable(String source, Integer extractable) {
        if (extractable == null || (extractable != 0 && extractable != 1)) {
            throw new BizException("extractable 只能为 0 或 1");
        }

        LambdaUpdateWrapper<Question> uw = new LambdaUpdateWrapper<Question>()
                .set(Question::getCanExtract, extractable);

        if (StrUtil.isBlank(source)) {
            // "未分类"：source 为 null 或空字符串
            uw.and(w -> w.isNull(Question::getSource).or().eq(Question::getSource, ""));
        } else {
            uw.eq(Question::getSource, source);
        }

        return questionMapper.update(null, uw);
    }

    @Override
    @Transactional
    public void updateExtractable(Long id, Integer extractable) {
        if (id == null) throw new BizException("id 不能为空");
        if (extractable == null || (extractable != 0 && extractable != 1)) {
            throw new BizException("extractable 只能为 0 或 1");
        }
        Question q = new Question();
        q.setId(id);
        q.setCanExtract(extractable);
        questionMapper.updateById(q);
    }

    @Override
    public QuestionPracticeVO preview(Long id) {
        Question q = questionMapper.selectById(id);
        if (q == null) throw new BizException("题目不存在");
        QuestionPracticeVO vo = new QuestionPracticeVO();
        BeanUtil.copyProperties(q, vo);
        // QuestionPracticeVO 本身不含 correctOption / analysis，天然安全
        return vo;
    }


    @Override
    public List<CategoryTreeVO> categoryTreeWithCount() {
        // 1. 一次 SQL 拿到所有 value 路径的题数
        List<Map<String, Object>> raw = questionMapper.selectCategoryCountGroupBy();
        Map<String, Long> countMap = new HashMap<>();
        if (raw != null) {
            for (Map<String, Object> row : raw) {
                Object catObj = row.get("category");
                Object cntObj = row.get("cnt");
                if (catObj == null || cntObj == null) {
                    continue;
                }
                String cat = catObj.toString();
                if (StrUtil.isBlank(cat)) {
                    continue;
                }
                countMap.put(cat, ((Number) cntObj).longValue());
            }
        }

        // 2. 拿字典树
        List<DictVO> tree = dictService.treeByType("question_category");

        // 3. 递归构建
        return buildCategoryTree(tree, countMap, "", "");
    }

    /**
     * 递归构建分类树
     *
     * @param nodes       当前层节点列表
     * @param countMap    value 路径 → 题数
     * @param parentValue 父级 value 路径
     * @param parentLabel 父级 label 路径
     */
    private List<CategoryTreeVO> buildCategoryTree(
            List<DictVO> nodes,
            Map<String, Long> countMap,
            String parentValue,
            String parentLabel) {

        List<CategoryTreeVO> result = new ArrayList<>();
        if (nodes == null || nodes.isEmpty()) {
            return result;
        }

        for (DictVO node : nodes) {
            CategoryTreeVO vo = new CategoryTreeVO();
            vo.setId(node.getId());
            vo.setDictValue(node.getDictValue());
            vo.setDictLabel(node.getDictLabel());

            String valuePath = StrUtil.isBlank(parentValue)
                    ? node.getDictValue()
                    : parentValue + "/" + node.getDictValue();
            String labelPath = StrUtil.isBlank(parentLabel)
                    ? node.getDictLabel()
                    : parentLabel + "/" + node.getDictLabel();

            vo.setPath(valuePath);
            vo.setLabelPath(labelPath);

            // 子节点
            List<CategoryTreeVO> children = buildCategoryTree(
                    node.getChildren(), countMap, valuePath, labelPath);
            vo.setChildren(children);

            // 自身题数 + 所有子类题数
            long selfCount = countMap.getOrDefault(valuePath, 0L);
            long subCount = children.stream()
                    .mapToLong(c -> c.getCount() == null ? 0L : c.getCount())
                    .sum();
            vo.setCount(selfCount + subCount);

            result.add(vo);
        }
        return result;
    }
}