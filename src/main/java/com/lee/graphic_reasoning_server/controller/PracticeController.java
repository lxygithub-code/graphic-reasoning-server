package com.lee.graphic_reasoning_server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lee.graphic_reasoning_server.common.BizException;
import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.common.R;
import com.lee.graphic_reasoning_server.common.UserContext;
import com.lee.graphic_reasoning_server.dto.CommentSaveDTO;
import com.lee.graphic_reasoning_server.dto.PracticeSingleDTO;
import com.lee.graphic_reasoning_server.dto.PracticeSubmitDTO;
import com.lee.graphic_reasoning_server.mapper.PracticeDetailMapper;
import com.lee.graphic_reasoning_server.mapper.QuestionMapper;
import com.lee.graphic_reasoning_server.po.PracticeDetail;
import com.lee.graphic_reasoning_server.po.Question;
import com.lee.graphic_reasoning_server.service.PdfExportService;
import com.lee.graphic_reasoning_server.service.PracticeService;
import com.lee.graphic_reasoning_server.service.QuestionService;
import com.lee.graphic_reasoning_server.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;
    private final PdfExportService pdfExportService;
    private final QuestionMapper questionMapper;
    private final PracticeDetailMapper detailMapper;
    private final QuestionService questionService;

    /** 背题模式：单题核对 */
    @PostMapping("/submit-single")
    public R<PracticeSingleVO> submitSingle(@Valid @RequestBody PracticeSingleDTO dto) {
        return R.ok(practiceService.submitSingle(dto));
    }

    /** 刷题模式：整组交卷 */
    @PostMapping("/submit-exam")
    public R<PracticeSubmitVO> submitExam(@Valid @RequestBody PracticeSubmitDTO dto) {
        return R.ok(practiceService.submitExam(dto));
    }

    @GetMapping("/{recordId}/detail")
    public R<PracticeRecordDetailVO> detail(@PathVariable Long recordId) {
        return R.ok(practiceService.getRecordDetail(recordId));
    }

    @PostMapping("/comment")
    public R<CommentVO> submitComment(@Valid @RequestBody CommentSaveDTO dto) {
        return R.ok(practiceService.submitComment(dto));
    }

    @GetMapping("/comment/list")
    public R<List<CommentVO>> listComments(
            @RequestParam Long questionId,
            @RequestParam(defaultValue = "10") Integer limit) {
        return R.ok(practiceService.listComments(questionId, limit));
    }

    /** 我的答题记录列表（分页） */
    @GetMapping("/records")
    public R<PageVO<PracticeRecordVO>> myRecords(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(practiceService.myRecords(pageNum, pageSize));
    }


    /** 我的错题列表 */
    @GetMapping("/wrong/list")
    public R<PageVO<QuestionDetailVO>> wrongList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(practiceService.wrongList(pageNum, pageSize));
    }

    /** 移出错题 */
    @DeleteMapping("/wrong/{questionId}")
    public R<Void> removeWrong(@PathVariable Long questionId) {
        practiceService.removeWrong(questionId);
        return R.ok();
    }

    /** 错题详情 */
    @GetMapping("/wrong/{questionId}/detail")
    public R<WrongQuestionDetailVO> wrongDetail(@PathVariable Long questionId) {
        return R.ok(practiceService.getWrongDetail(questionId));
    }

    @PostMapping("/wrong/export-pdf")
    public R<String> exportWrongPdf(@RequestBody List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            throw new BizException("请选择需要导出的题目");
        }
        // ★ 限制最多 20 题
        if (questionIds.size() > 20) {
            throw new BizException("一次最多只能导出 20 道题哦~");
        }

        Long userId = UserContext.get();
        if (userId == null) throw new BizException("未登录");

        // 1. 查题目
        List<Question> questions = questionMapper.selectByIds(questionIds);
        if (questions.isEmpty()) throw new BizException("题目不存在");

        // 2. 查用户最近一次的作答记录（用于获取错选和时长）
        List<PracticeDetail> details = detailMapper.selectList(
                new LambdaQueryWrapper<PracticeDetail>()
                        .eq(PracticeDetail::getUserId, userId)
                        .in(PracticeDetail::getQuestionId, questionIds)
                        .orderByDesc(PracticeDetail::getId)
        );
        // 去重取最新的一条
        Map<Long, PracticeDetail> detailMap = new HashMap<>();
        for (PracticeDetail detail : details) {
            detailMap.putIfAbsent(detail.getQuestionId(), detail);
        }

        // 3. 组装数据
        List<WrongPdfItemVO> pdfItems = new ArrayList<>();
        for (Question q : questions) {
            WrongPdfItemVO item = new WrongPdfItemVO();
            item.setQuestion(q);

            PracticeDetail detail = detailMap.get(q.getId());
            if (detail != null) {
                item.setUserOption(detail.getUserOption());
                item.setDuration(detail.getDuration());
            }

            // 查解析
            item.setAnalyses(questionService.listAnalyses(q.getId()));
            pdfItems.add(item);
        }

        String pdfUrl = pdfExportService.generateWrongPdf(pdfItems);
        return R.ok(pdfUrl);
    }

    @GetMapping("/wrong/yesterday-ranking")
    public R<List<WrongRankingVO>> yesterdayWrongRanking() {
        return R.ok(practiceService.getYesterdayWrongRanking());
    }
}
