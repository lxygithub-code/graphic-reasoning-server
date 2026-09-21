package com.lee.graphic_reasoning_server.controller;

import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.common.R;
import com.lee.graphic_reasoning_server.dto.CommentSaveDTO;
import com.lee.graphic_reasoning_server.dto.PracticeSingleDTO;
import com.lee.graphic_reasoning_server.dto.PracticeSubmitDTO;
import com.lee.graphic_reasoning_server.service.PracticeService;
import com.lee.graphic_reasoning_server.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

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
}
