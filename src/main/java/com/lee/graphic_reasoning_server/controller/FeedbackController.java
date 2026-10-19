package com.lee.graphic_reasoning_server.controller;

import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.common.R;
import com.lee.graphic_reasoning_server.dto.FeedbackSubmitDTO;
import com.lee.graphic_reasoning_server.service.FeedbackService;
import com.lee.graphic_reasoning_server.vo.FeedbackVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    /** 提交意见 */
    @PostMapping("/submit")
    public R<Long> submit(@Valid @RequestBody FeedbackSubmitDTO dto) {
        return R.ok(feedbackService.submit(dto));
    }

    /** 我的意见列表 */
    @GetMapping("/my")
    public R<PageVO<FeedbackVO>> my(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(feedbackService.myList(pageNum, pageSize));
    }

    /** 意见详情 */
    @GetMapping("/{id}")
    public R<FeedbackVO> detail(@PathVariable Long id) {
        return R.ok(feedbackService.detail(id));
    }

    /** 未读回复数 */
    @GetMapping("/unread-count")
    public R<Long> unreadCount() {
        return R.ok(feedbackService.unreadCount());
    }

}
