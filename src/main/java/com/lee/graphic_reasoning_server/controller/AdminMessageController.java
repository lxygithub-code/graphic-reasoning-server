package com.lee.graphic_reasoning_server.controller;

import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.common.R;
import com.lee.graphic_reasoning_server.service.AdminMessageService;
import com.lee.graphic_reasoning_server.vo.AdminMessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/message")
@RequiredArgsConstructor
public class AdminMessageController {

    private final AdminMessageService messageService;

    /** 消息分页 */
    @GetMapping("/page")
    public R<PageVO<AdminMessageVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer isRead) {
        return R.ok(messageService.page(pageNum, pageSize, isRead));
    }

    /** 未读数 */
    @GetMapping("/unread-count")
    public R<Long> unreadCount() {
        return R.ok(messageService.unreadCount());
    }

    /** 标记单条已读 */
    @PutMapping("/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        messageService.markRead(id);
        return R.ok();
    }

    /** 全部已读 */
    @PutMapping("/read-all")
    public R<Void> markAllRead() {
        messageService.markAllRead();
        return R.ok();
    }
}