package com.lee.graphic_reasoning_server.service;

import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.vo.AdminMessageVO;

public interface AdminMessageService {

    /** 消息分页（含群发消息） */
    PageVO<AdminMessageVO> page(Integer pageNum, Integer pageSize, Integer isRead);

    /** 未读数（当前管理员 + 群发） */
    Long unreadCount();

    /** 标记单条已读 */
    void markRead(Long id);

    /** 全部已读 */
    void markAllRead();

    /** 新增消息（内部调用） */
    void pushMessage(Long adminId, String type, String title, String content,
                     Long refId, Long refQuestionId);
}