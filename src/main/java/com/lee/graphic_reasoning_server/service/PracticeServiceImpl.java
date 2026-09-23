package com.lee.graphic_reasoning_server.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lee.graphic_reasoning_server.common.BizException;
import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.common.UserContext;
import com.lee.graphic_reasoning_server.dto.CommentSaveDTO;
import com.lee.graphic_reasoning_server.dto.PracticeSingleDTO;
import com.lee.graphic_reasoning_server.dto.PracticeSubmitDTO;
import com.lee.graphic_reasoning_server.po.*;
import com.lee.graphic_reasoning_server.mapper.*;
import com.lee.graphic_reasoning_server.service.PracticeService;
import com.lee.graphic_reasoning_server.util.ContentFilter;
import com.lee.graphic_reasoning_server.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PracticeServiceImpl implements PracticeService {

    private final QuestionMapper questionMapper;
    private final PracticeRecordMapper recordMapper;
    private final PracticeDetailMapper detailMapper;
    private final WrongQuestionMapper wrongMapper;
    private final QuestionCommentMapper commentMapper;
    private final UserMapper userMapper;
    private final UserQuestionMapper userQuestionMapper;
    private final QuestionService questionService;
    private final QuestionAnalysisMapper analysisMapper;


    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 背题模式：单题核对 ====================
    @Override
    public PracticeSingleVO submitSingle(PracticeSingleDTO dto) {
        Long userId = UserContext.get();
        if (userId == null) throw new BizException("未登录");

        Question q = questionMapper.selectById(dto.getQuestionId());
        if (q == null) throw new BizException("题目不存在");

        boolean isCorrect = StrUtil.isNotBlank(dto.getUserAnswer())
                && dto.getUserAnswer().equalsIgnoreCase(q.getCorrectOption());

        UserQuestion uq = userQuestionMapper.selectOne(
                new LambdaQueryWrapper<UserQuestion>()
                        .eq(UserQuestion::getUserId, userId)
                        .eq(UserQuestion::getQuestionId, q.getId()));
        if (uq == null) {
            uq = new UserQuestion();
            uq.setUserId(userId);
            uq.setQuestionId(q.getId());
            uq.setIsCorrect(isCorrect ? 1 : 0);
            uq.setAnswerCount(1);
            uq.setLastAnswerTime(LocalDateTime.now());
            userQuestionMapper.insert(uq);
        } else {
            uq.setIsCorrect(isCorrect ? 1 : 0);
            uq.setAnswerCount(uq.getAnswerCount() + 1);
            uq.setLastAnswerTime(LocalDateTime.now());
            userQuestionMapper.updateById(uq);
        }

        // 答错写错题本
        if (!isCorrect && StrUtil.isNotBlank(dto.getUserAnswer())) {
            upsertWrong(userId, q.getId());
        }

        // ★ 查当前用户的评论权限
        User user = userMapper.selectById(userId);
        Integer canComment = (user != null && user.getCanComment() != null)
                ? user.getCanComment() : 1;

        PracticeSingleVO vo = new PracticeSingleVO();
        vo.setIsCorrect(isCorrect);
        vo.setCorrectOption(q.getCorrectOption());
        vo.setAnalyses(loadAnalyses(q.getId()));
        vo.setComments(loadComments(q.getId()));
        vo.setAnalyses(questionService.listAnalyses(q.getId()));
        vo.setCanComment(canComment);
        return vo;
    }

    /** ★ 查分平台解析 */
    private List<QuestionDetailVO.AnalysisVO> loadAnalyses(Long questionId) {
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

    // ==================== 刷题模式：整组交卷 ====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PracticeSubmitVO submitExam(PracticeSubmitDTO dto) {
        Long userId = UserContext.get();
        if (userId == null) throw new BizException("未登录");

        List<PracticeSubmitDTO.Item> items = dto.getAnswers();
        if (items == null || items.isEmpty()) throw new BizException("答题记录为空");

        // 1. 建汇总记录（先占位拿自增 id）
        PracticeRecord record = new PracticeRecord();
        record.setUserId(userId);
        record.setCategory(dto.getExamType());
        record.setTotalCount(items.size());
        record.setCorrectCount(0);
        record.setAccuracy(BigDecimal.ZERO);
        record.setTotalDuration(0);
        record.setCreateTime(LocalDateTime.now());
        recordMapper.insert(record);

        // 2. 批量查题目（一次查询）
        List<Long> qIds = items.stream().map(PracticeSubmitDTO.Item::getQuestionId).collect(Collectors.toList());
        List<Question> questions = questionMapper.selectBatchIds(qIds);
        Map<Long, Question> qMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        // 3. 遍历每题，统计
        int correctCount = 0;
        int totalDuration = 0;
        List<PracticeDetail> details = new ArrayList<>();

        for (PracticeSubmitDTO.Item it : items) {
            Question q = qMap.get(it.getQuestionId());
            if (q == null) continue;

            boolean isCorrect = StrUtil.isNotBlank(it.getUserAnswer())
                    && it.getUserAnswer().equalsIgnoreCase(q.getCorrectOption());
            if (isCorrect) correctCount++;
            totalDuration += (it.getTimeSpent() == null ? 0 : it.getTimeSpent());

            PracticeDetail d = new PracticeDetail();
            d.setRecordId(record.getId());
            d.setUserId(userId);
            d.setQuestionId(q.getId());
            d.setUserOption(StrUtil.nullToEmpty(it.getUserAnswer()));
            d.setCorrectOption(q.getCorrectOption());
            d.setIsCorrect(isCorrect ? 1 : 0);
            d.setDuration(it.getTimeSpent() == null ? 0 : it.getTimeSpent());
            d.setCreateTime(LocalDateTime.now());
            details.add(d);

            // 答错的写到错题本
            if (!isCorrect) {
                upsertWrong(userId, q.getId());
            }
        }

        // 4. 批量插入明细
        if (!details.isEmpty()) {
            detailMapper.batchInsert(details);
        }

        // 5. 回填汇总
        BigDecimal accuracy = BigDecimal.valueOf(correctCount * 100.0 / items.size())
                .setScale(2, RoundingMode.HALF_UP);
        record.setCorrectCount(correctCount);
        record.setAccuracy(accuracy);
        record.setTotalDuration(totalDuration);
        recordMapper.updateById(record);

        // 6. 返回
        PracticeSubmitVO vo = new PracticeSubmitVO();
        vo.setRecordId(record.getId());
        vo.setTotalCount(items.size());
        vo.setCorrectCount(correctCount);
        vo.setAccuracy(accuracy.doubleValue());
        vo.setTotalTime(totalDuration);
        return vo;
    }

    // ==================== 辅助 ====================
    private void upsertWrong(Long userId, Long questionId) {
        WrongQuestion w = wrongMapper.selectOne(
                new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId)
                        .eq(WrongQuestion::getQuestionId, questionId));
        if (w == null) {
            w = new WrongQuestion();
            w.setUserId(userId);
            w.setQuestionId(questionId);
            w.setWrongCount(1);
            w.setLastWrongTime(LocalDateTime.now());
            wrongMapper.insert(w);
        } else {
            w.setWrongCount(w.getWrongCount() + 1);
            w.setLastWrongTime(LocalDateTime.now());
            wrongMapper.updateById(w);
        }
    }

    private List<CommentVO> loadComments(Long questionId) {
        List<QuestionComment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<QuestionComment>()
                        .eq(QuestionComment::getQuestionId, questionId)
                        .orderByDesc(QuestionComment::getLikeCount)
                        .orderByDesc(QuestionComment::getCreateTime)
                        .last("LIMIT 10"));

        if (comments.isEmpty()) return Collections.emptyList();

        // 批量查评论者
        List<Long> userIds = comments.stream()
                .map(QuestionComment::getUserId)
                .distinct()
                .collect(Collectors.toList());
        List<User> users = userMapper.selectByIds(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // 过滤掉 showComment = 0 的用户评论
        return comments.stream()
                .filter(c -> {
                    User author = userMap.get(c.getUserId());
                    if (author == null) return false;
                    Integer show = author.getShowComment();
                    // 默认展示，只有明确为 0 才隐藏
                    return show == null || show == 1;
                })
                .map(c -> {
                    User author = userMap.get(c.getUserId());
                    CommentVO vo = new CommentVO();
                    vo.setId(c.getId());
                    vo.setNickname(StrUtil.blankToDefault(author.getNickname(), "匿名用户"));
                    vo.setContent(c.getContent());
                    vo.setLikeCount(c.getLikeCount());
                    vo.setCreateTime(c.getCreateTime() == null ? null : c.getCreateTime().format(FMT));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public PracticeRecordDetailVO getRecordDetail(Long recordId) {
        Long userId = UserContext.get();
        if (userId == null) throw new BizException("未登录");

        PracticeRecord record = recordMapper.selectById(recordId);
        if (record == null) throw new BizException("练习记录不存在");
        if (!record.getUserId().equals(userId)) throw new BizException("无权查看该记录");

        PracticeRecordDetailVO vo = new PracticeRecordDetailVO();
        vo.setRecordId(record.getId());
        vo.setExamType(record.getCategory());
        vo.setTotalCount(record.getTotalCount());
        vo.setCorrectCount(record.getCorrectCount());
        vo.setAccuracy(record.getAccuracy() == null ? 0.0 : record.getAccuracy().doubleValue());
        vo.setTotalDuration(record.getTotalDuration());
        if (record.getCreateTime() != null) {
            vo.setCreateTime(record.getCreateTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        List<PracticeDetail> details = detailMapper.selectList(
                new LambdaQueryWrapper<PracticeDetail>()
                        .eq(PracticeDetail::getRecordId, recordId)
                        .orderByAsc(PracticeDetail::getId));
        if (details.isEmpty()) {
            vo.setItems(Collections.emptyList());
            return vo;
        }

        // 批量查题目
        List<Long> qIds = details.stream()
                .map(PracticeDetail::getQuestionId)
                .distinct()
                .collect(Collectors.toList());
        List<Question> questions = questionMapper.selectBatchIds(qIds);
        Map<Long, Question> qMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        List<PracticeDetailItemVO> items = new ArrayList<>();
        for (PracticeDetail d : details) {
            PracticeDetailItemVO item = new PracticeDetailItemVO();
            item.setQuestionId(d.getQuestionId());
            item.setUserOption(d.getUserOption());
            item.setCorrectOption(d.getCorrectOption());
            item.setIsCorrect(d.getIsCorrect() != null && d.getIsCorrect() == 1);
            item.setDuration(d.getDuration());

            Question q = qMap.get(d.getQuestionId());
            if (q != null) {
                item.setContent(q.getContent());
                item.setImageUrl(q.getImageUrl());
                item.setOptions(q.getOptions());
                item.setAnalysis(q.getAnalysis());
                item.setAnalyses(loadAnalyses(q.getId()));
                item.setSource(q.getSource());
                item.setExamType(q.getExamType());
                item.setExamSubType(q.getExamSubType());
            }
            items.add(item);
        }
        vo.setItems(items);
        return vo;
    }

    private final ContentFilter contentFilter;
    private final StringRedisTemplate redis;

    @Override
    @Transactional
    public CommentVO submitComment(CommentSaveDTO dto) {
        Long userId = UserContext.get();
        if (userId == null) throw new BizException("未登录");

        // 校验用户是否允许评论
        User uc = userMapper.selectById(userId);
        if (uc == null) throw new BizException("用户不存在");
        if (uc.getCanComment() != null && uc.getCanComment() == 0) {
            throw new BizException("您的评论权限已被关闭");
        }

        // 限流：10 秒 1 条
        String rateKey = "comment:rate:" + userId;
        Boolean ok = redis.opsForValue().setIfAbsent(rateKey, "1", Duration.ofSeconds(10));
        if (Boolean.FALSE.equals(ok)) {
            throw new BizException("评论太频繁，请稍后再试");
        }

        // 防重复：5 分钟内不能发相同内容
        String contentKey = "comment:content:" + userId + ":" + dto.getContent().hashCode();
        Boolean ok2 = redis.opsForValue().setIfAbsent(contentKey, "1", Duration.ofMinutes(5));
        if (Boolean.FALSE.equals(ok2)) {
            throw new BizException("请勿重复发布相同内容");
        }

        // 1. 校验内容
        String reason = contentFilter.validate(dto.getContent());
        if (reason != null) throw new BizException(reason);

        // 2. 校验题目存在
        Question q = questionMapper.selectById(dto.getQuestionId());
        if (q == null) throw new BizException("题目不存在");

        // 3. 插入
        QuestionComment comment = new QuestionComment();
        comment.setQuestionId(dto.getQuestionId());
        comment.setUserId(userId);
        comment.setContent(dto.getContent().trim());
        comment.setLikeCount(0);
        commentMapper.insert(comment);

        // 4. 返回 VO
        User u = userMapper.selectById(userId);
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setNickname(u != null && StrUtil.isNotBlank(u.getNickname()) ? u.getNickname() : "匿名用户");
        vo.setContent(comment.getContent());
        vo.setLikeCount(0);
        vo.setCreateTime(LocalDateTime.now().format(FMT));
        return vo;
    }

    @Override
    public List<CommentVO> listComments(Long questionId, Integer limit) {
        if (limit == null || limit <= 0 || limit > 50) limit = 10;
        List<QuestionComment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<QuestionComment>()
                        .eq(QuestionComment::getQuestionId, questionId)
                        .orderByDesc(QuestionComment::getLikeCount)
                        .orderByDesc(QuestionComment::getCreateTime)
                        .last("LIMIT " + limit));

        if (comments.isEmpty()) return Collections.emptyList();

        List<Long> userIds = comments.stream().map(QuestionComment::getUserId).distinct().collect(Collectors.toList());
        Map<Long, String> nicknameMap = userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> StrUtil.blankToDefault(u.getNickname(), "匿名用户")));

        return comments.stream().map(c -> {
            CommentVO vo = new CommentVO();
            vo.setId(c.getId());
            vo.setNickname(nicknameMap.getOrDefault(c.getUserId(), "匿名用户"));
            vo.setContent(c.getContent());
            vo.setLikeCount(c.getLikeCount());
            vo.setCreateTime(c.getCreateTime() == null ? null : c.getCreateTime().format(FMT));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public PageVO<PracticeRecordVO> myRecords(Integer pageNum, Integer pageSize) {
        Long userId = UserContext.get();
        if (userId == null) throw new BizException("未登录");

        Page<PracticeRecord> page = recordMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<PracticeRecord>()
                        .eq(PracticeRecord::getUserId, userId)
                        .orderByDesc(PracticeRecord::getCreateTime));

        List<PracticeRecordVO> list = page.getRecords().stream().map(r -> {
            PracticeRecordVO vo = new PracticeRecordVO();
            BeanUtil.copyProperties(r, vo);
            vo.setAccuracy(r.getAccuracy() == null ? 0.0 : r.getAccuracy().doubleValue());
            if (r.getCreateTime() != null) {
                vo.setCreateTime(r.getCreateTime().format(FMT));
            }
            return vo;
        }).collect(Collectors.toList());

        return PageVO.of(page.getTotal(), page.getCurrent(), page.getSize(), list);
    }

    @Override
    public PageVO<QuestionDetailVO> wrongList(Integer pageNum, Integer pageSize) {
        Long userId = UserContext.get();
        if (userId == null) throw new BizException("未登录");

        Page<WrongQuestion> page = wrongMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId)
                        .orderByDesc(WrongQuestion::getLastWrongTime));

        List<Long> qIds = page.getRecords().stream()
                .map(WrongQuestion::getQuestionId)
                .collect(Collectors.toList());

        List<QuestionDetailVO> records = new ArrayList<>();
        if (!qIds.isEmpty()) {
            List<Question> qs = questionMapper.selectByIds(qIds);
            Map<Long, Question> map = qs.stream()
                    .collect(Collectors.toMap(Question::getId, Function.identity()));
            for (Long qid : qIds) {
                Question q = map.get(qid);
                if (q != null) {
                    QuestionDetailVO vo = new QuestionDetailVO();
                    BeanUtil.copyProperties(q, vo);
                    records.add(vo);
                }
            }
        }
        return PageVO.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public void removeWrong(Long questionId) {
        Long userId = UserContext.get();
        if (userId == null) throw new BizException("未登录");
        wrongMapper.delete(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .eq(WrongQuestion::getQuestionId, questionId));
    }


    @Override
    public WrongQuestionDetailVO getWrongDetail(Long questionId) {
        Long userId = UserContext.get();
        if (userId == null) throw new BizException("未登录");

        // 1. 查题目
        Question q = questionMapper.selectById(questionId);
        if (q == null) throw new BizException("题目不存在");

        // 2. 查错题记录
        WrongQuestion wrong = wrongMapper.selectOne(
                new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId)
                        .eq(WrongQuestion::getQuestionId, questionId));

        // 3. 查最后一次答错的明细
        PracticeDetail lastDetail = detailMapper.selectOne(
                new LambdaQueryWrapper<PracticeDetail>()
                        .eq(PracticeDetail::getUserId, userId)
                        .eq(PracticeDetail::getQuestionId, questionId)
                        .eq(PracticeDetail::getIsCorrect, 0)
                        .orderByDesc(PracticeDetail::getId)
                        .last("LIMIT 1"));

        // 4. 组装
        WrongQuestionDetailVO vo = new WrongQuestionDetailVO();
        vo.setQuestionId(q.getId());
        vo.setContent(q.getContent());
        vo.setImageUrl(q.getImageUrl());
        vo.setOptions(q.getOptions());
        vo.setCorrectOption(q.getCorrectOption());
        vo.setCategory(q.getCategory());
        vo.setExamType(q.getExamType());
        vo.setSource(q.getSource());
        vo.setDifficulty(q.getDifficulty());
        vo.setAnalysis(StrUtil.blankToDefault(q.getAnalysis(), ""));
        vo.setAnalyses(loadAnalyses(q.getId()));

        if (wrong != null) {
            vo.setWrongCount(wrong.getWrongCount());
            if (wrong.getLastWrongTime() != null) {
                vo.setLastWrongTime(wrong.getLastWrongTime().format(FMT));
            }
        }

        if (lastDetail != null) {
            vo.setUserOption(lastDetail.getUserOption());
            vo.setDuration(lastDetail.getDuration());
            if (lastDetail.getCreateTime() != null) {
                vo.setAnswerTime(lastDetail.getCreateTime().format(FMT));
            }
        }

        return vo;
    }
}
