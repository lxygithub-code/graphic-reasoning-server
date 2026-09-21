package com.lee.graphic_reasoning_server.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lee.graphic_reasoning_server.common.BizException;
import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.common.R;
import com.lee.graphic_reasoning_server.common.UserContext;
import com.lee.graphic_reasoning_server.dto.FavoriteToggleDTO;
import com.lee.graphic_reasoning_server.mapper.FavoriteMapper;
import com.lee.graphic_reasoning_server.mapper.QuestionAnalysisMapper;
import com.lee.graphic_reasoning_server.mapper.QuestionMapper;
import com.lee.graphic_reasoning_server.po.Favorite;
import com.lee.graphic_reasoning_server.po.Question;
import com.lee.graphic_reasoning_server.po.QuestionAnalysis;
import com.lee.graphic_reasoning_server.vo.QuestionDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteMapper favoriteMapper;
    private final QuestionMapper questionMapper;
    private final QuestionAnalysisMapper questionAnalysisMapper;

    /**
     * 收藏/取消收藏
     */
    @PostMapping("/toggle")
    public R<Boolean> toggle(@RequestBody FavoriteToggleDTO dto) {
        Long questionId = dto.getQuestionId();
        if (questionId == null) throw new BizException("questionId 不能为空");

        Long userId = UserContext.get();
        if (userId == null) throw new BizException("未登录");

        Favorite exist = favoriteMapper.selectOne(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getQuestionId, questionId));

        if (exist != null) {
            favoriteMapper.deleteById(exist.getId());
            return R.ok(false);
        }
        Favorite fav = new Favorite();
        fav.setUserId(userId);
        fav.setQuestionId(questionId);
        favoriteMapper.insert(fav);
        return R.ok(true);
    }

    /**
     * 是否已收藏
     */
    @GetMapping("/check")
    public R<Boolean> check(@RequestParam Long questionId) {
        Long userId = UserContext.get();
        if (userId == null) return R.ok(false);
        Long count = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getQuestionId, questionId));
        return R.ok(count != null && count > 0);
    }

    /**
     * 收藏列表（分页）
     */
    @GetMapping("/list")
    public R<PageVO<QuestionDetailVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = UserContext.get();
        if (userId == null) throw new BizException("未登录");

        Page<Favorite> page = favoriteMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreateTime));

        List<Long> qIds = page.getRecords().stream()
                .map(Favorite::getQuestionId)
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
        return R.ok(PageVO.of(page.getTotal(), page.getCurrent(), page.getSize(), records));
    }

    /** 收藏详情 */
    @GetMapping("/{questionId}/detail")
    public R<QuestionDetailVO> detail(@PathVariable Long questionId) {
        Long userId = UserContext.get();
        if (userId == null) throw new BizException("未登录");

        // 校验是否已收藏（可选）
        Long count = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getQuestionId, questionId));
        if (count == null || count == 0) throw new BizException("该题目未收藏");

        Question q = questionMapper.selectById(questionId);
        if (q == null) throw new BizException("题目不存在");

        QuestionDetailVO vo = new QuestionDetailVO();
        BeanUtil.copyProperties(q, vo);

        // 分平台解析
        List<QuestionAnalysis> analyses = questionAnalysisMapper.selectList(
                new LambdaQueryWrapper<QuestionAnalysis>()
                        .eq(QuestionAnalysis::getQuestionId, questionId)
                        .orderByAsc(QuestionAnalysis::getSort)
                        .orderByAsc(QuestionAnalysis::getId));
        vo.setAnalyses(analyses.stream().map(a -> {
            QuestionDetailVO.AnalysisVO av = new QuestionDetailVO.AnalysisVO();
            av.setPlatform(a.getPlatform());
            av.setType(StrUtil.blankToDefault(a.getType(), "text"));
            av.setContent(a.getContent());
            return av;
        }).collect(Collectors.toList()));

        return R.ok(vo);
    }
}