package com.lee.graphic_reasoning_server.controller;

import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.common.R;
import com.lee.graphic_reasoning_server.dto.CommentQueryDTO;
import com.lee.graphic_reasoning_server.service.AdminCommentService;
import com.lee.graphic_reasoning_server.vo.CommentQuestionDetailVO;
import com.lee.graphic_reasoning_server.vo.CommentQuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/comment")
@RequiredArgsConstructor
public class AdminCommentController {

    private final AdminCommentService adminCommentService;

    /** 有评论的题目列表（分页） */
    @PostMapping("/question-page")
    public R<PageVO<CommentQuestionVO>> questionPage(@RequestBody CommentQueryDTO dto) {
        return R.ok(adminCommentService.questionPage(dto));
    }

    /** 某题目的评论详情 */
    @GetMapping("/question/{questionId}")
    public R<CommentQuestionDetailVO> questionDetail(@PathVariable Long questionId) {
        return R.ok(adminCommentService.questionDetail(questionId));
    }

    /** 隐藏 / 恢复评论 */
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminCommentService.updateStatus(id, status);
        return R.ok();
    }

    /** 删除评论 */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        adminCommentService.delete(id);
        return R.ok();
    }
}