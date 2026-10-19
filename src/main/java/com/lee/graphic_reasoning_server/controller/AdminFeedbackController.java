package com.lee.graphic_reasoning_server.controller;

import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.common.R;
import com.lee.graphic_reasoning_server.dto.FeedbackQueryDTO;
import com.lee.graphic_reasoning_server.dto.FeedbackReplyDTO;
import com.lee.graphic_reasoning_server.service.FeedbackService;
import com.lee.graphic_reasoning_server.vo.AdminFeedbackVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/feedback")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping("/page")
    public R<PageVO<AdminFeedbackVO>> page(@RequestBody FeedbackQueryDTO dto) {
        return R.ok(feedbackService.adminPage(dto));
    }

    @GetMapping("/{id}")
    public R<AdminFeedbackVO> detail(@PathVariable Long id) {
        return R.ok(feedbackService.adminDetail(id));
    }

    @PutMapping("/{id}/reply")
    public R<Void> reply(@PathVariable Long id,
                         @Valid @RequestBody FeedbackReplyDTO dto) {
        feedbackService.reply(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        feedbackService.delete(id);
        return R.ok();
    }

    /** 更新状态（0待处理 1处理中 2已解决） */
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        feedbackService.updateStatus(id, status);
        return R.ok();
    }
}