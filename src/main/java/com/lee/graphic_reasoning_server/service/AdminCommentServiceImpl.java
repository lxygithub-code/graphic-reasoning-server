package com.lee.graphic_reasoning_server.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lee.graphic_reasoning_server.common.BizException;
import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.dto.CommentQueryDTO;
import com.lee.graphic_reasoning_server.mapper.QuestionCommentMapper;
import com.lee.graphic_reasoning_server.mapper.QuestionMapper;
import com.lee.graphic_reasoning_server.mapper.UserMapper;
import com.lee.graphic_reasoning_server.po.Question;
import com.lee.graphic_reasoning_server.po.QuestionComment;
import com.lee.graphic_reasoning_server.po.User;
import com.lee.graphic_reasoning_server.util.DictTranslator;
import com.lee.graphic_reasoning_server.vo.CommentItemVO;
import com.lee.graphic_reasoning_server.vo.CommentQuestionDetailVO;
import com.lee.graphic_reasoning_server.vo.CommentQuestionVO;
import com.lee.graphic_reasoning_server.vo.QuestionDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCommentServiceImpl implements AdminCommentService {

    private final QuestionCommentMapper commentMapper;
    private final QuestionMapper questionMapper;
    private final UserMapper userMapper;
    private final QuestionService questionService;
    private final DictTranslator dictTranslator;

    // ==================== 列表：有评论的题目 ====================
    @Override
    public PageVO<CommentQuestionVO> questionPage(CommentQueryDTO dto) {
        int pageNum = dto.getPageNum() == null || dto.getPageNum() < 1 ? 1 : dto.getPageNum();
        int pageSize = dto.getPageSize() == null || dto.getPageSize() < 1 ? 10 : dto.getPageSize();

        // 1. 按 question_id 分组统计
        // SELECT question_id, COUNT(*) cnt, MAX(create_time) last_time
        // FROM t_question_comment
        // WHERE deleted = 0 [AND status = ?] [AND content LIKE ?]
        // GROUP BY question_id
        // ORDER BY last_time DESC
        QueryWrapper<QuestionComment> qw = new QueryWrapper<>();
        qw.select("question_id AS questionId",
                        "COUNT(*) AS commentCount",
                        "MAX(create_time) AS lastCommentTime")
                .eq("deleted", 0)
                .eq(dto.getStatus() != null, "status", dto.getStatus())
                .like(StrUtil.isNotBlank(dto.getKeyword()), "content", dto.getKeyword())
                .groupBy("question_id")
                .orderByDesc("lastCommentTime");

        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize);
        Page<Map<String, Object>> result = commentMapper.selectMapsPage(page, qw);

        if (result.getRecords().isEmpty()) {
            return PageVO.of(result.getTotal(), result.getCurrent(), result.getSize(),
                    Collections.emptyList());
        }

        // 2. 提取 questionId
        List<Long> qIds = result.getRecords().stream()
                .map(m -> ((Number) m.get("questionId")).longValue())
                .collect(Collectors.toList());

        // 3. 一次查题目
        Map<Long, Question> qMap = questionMapper.selectByIds(qIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        // 4. 每题取最新一条评论（批量：用 IN + 遍历）
        List<QuestionComment> allLatest = commentMapper.selectList(
                new LambdaQueryWrapper<QuestionComment>()
                        .in(QuestionComment::getQuestionId, qIds)
                        .eq(QuestionComment::getStatus, dto.getStatus() == null ? 1 : dto.getStatus())
                        .orderByDesc(QuestionComment::getCreateTime));
        Map<Long, QuestionComment> latestMap = new HashMap<>();
        for (QuestionComment c : allLatest) {
            latestMap.putIfAbsent(c.getQuestionId(), c);
        }

        // 5. 组装
        List<CommentQuestionVO> records = new ArrayList<>();
        for (Map<String, Object> row : result.getRecords()) {
            Long qid = ((Number) row.get("questionId")).longValue();
            Question q = qMap.get(qid);
            if (q == null) continue;

            CommentQuestionVO vo = new CommentQuestionVO();
            vo.setQuestionId(qid);
            vo.setContent(StrUtil.maxLength(q.getContent(), 100));
            vo.setImageUrl(q.getImageUrl());
            vo.setCategory(q.getCategory());
            vo.setCategoryLabel(dictTranslator.valuePathToLabelPath(q.getCategory()));
            vo.setSource(q.getSource());
            vo.setCommentCount(((Number) row.get("commentCount")).longValue());

            Object timeObj = row.get("lastCommentTime");
            if (timeObj instanceof java.sql.Timestamp ts) {
                vo.setLastCommentTime(ts.toLocalDateTime());
            } else if (timeObj instanceof java.time.LocalDateTime dt) {
                vo.setLastCommentTime(dt);
            }

            QuestionComment latest = latestMap.get(qid);
            if (latest != null) {
                vo.setLastCommentContent(StrUtil.maxLength(latest.getContent(), 60));
            }
            records.add(vo);
        }

        return PageVO.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    // ==================== 详情：题目 + 评论列表 ====================
    @Override
    public CommentQuestionDetailVO questionDetail(Long questionId) {
        if (questionId == null) throw new BizException("questionId 不能为空");

        Question q = questionMapper.selectById(questionId);
        if (q == null) throw new BizException("题目不存在");

        // 1. 题目 VO
        QuestionDetailVO questionVO = new QuestionDetailVO();
        BeanUtil.copyProperties(q, questionVO);
        questionVO.setCategoryLabel(dictTranslator.valuePathToLabelPath(q.getCategory()));
        questionVO.setAnalyses(questionService.listAnalyses(q.getId()));

        // 2. 全部评论（不过滤 status，管理员要看全）
        List<QuestionComment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<QuestionComment>()
                        .eq(QuestionComment::getQuestionId, questionId)
                        .orderByDesc(QuestionComment::getCreateTime));

        // 3. 批量查评论者
        Map<Long, User> userMap = Collections.emptyMap();
        if (!comments.isEmpty()) {
            List<Long> userIds = comments.stream()
                    .map(QuestionComment::getUserId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (!userIds.isEmpty()) {
                userMap = userMapper.selectByIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));
            }
        }

        // 4. 组装评论列表
        List<CommentItemVO> items = new ArrayList<>();
        for (QuestionComment c : comments) {
            CommentItemVO vo = new CommentItemVO();
            vo.setId(c.getId());
            vo.setUserId(c.getUserId());
            User u = userMap.get(c.getUserId());
            vo.setNickname(u != null ? StrUtil.blankToDefault(u.getNickname(), "匿名用户") : "匿名用户");
            vo.setAvatarUrl(u != null ? u.getAvatarUrl() : "");
            vo.setContent(c.getContent());
            vo.setLikeCount(c.getLikeCount() == null ? 0 : c.getLikeCount());
            vo.setStatus(c.getStatus() == null ? 1 : c.getStatus());
            vo.setCreateTime(c.getCreateTime());
            items.add(vo);
        }

        CommentQuestionDetailVO result = new CommentQuestionDetailVO();
        result.setQuestion(questionVO);
        result.setComments(items);
        return result;
    }

    // ==================== 隐藏 / 恢复 ====================
    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        if (id == null) throw new BizException("id 不能为空");
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("status 只能是 0 或 1");
        }

        QuestionComment exist = commentMapper.selectById(id);
        if (exist == null) throw new BizException("评论不存在");

        QuestionComment update = new QuestionComment();
        update.setId(id);
        update.setStatus(status);
        commentMapper.updateById(update);
    }

    // ==================== 删除 ====================
    @Override
    @Transactional
    public void delete(Long id) {
        if (id == null) throw new BizException("id 不能为空");
        QuestionComment exist = commentMapper.selectById(id);
        if (exist == null) throw new BizException("评论不存在");
        commentMapper.deleteById(id);
    }
}