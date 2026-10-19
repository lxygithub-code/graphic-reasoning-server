package com.lee.graphic_reasoning_server.service;
import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.dto.FeedbackQueryDTO;
import com.lee.graphic_reasoning_server.dto.FeedbackReplyDTO;
import com.lee.graphic_reasoning_server.dto.FeedbackSubmitDTO;
import com.lee.graphic_reasoning_server.vo.AdminFeedbackVO;
import com.lee.graphic_reasoning_server.vo.FeedbackVO;

public interface FeedbackService {

    // ========== 用户端 ==========
    /** 提交意见 */
    Long submit(FeedbackSubmitDTO dto);

    /** 我的意见列表 */
    PageVO<FeedbackVO> myList(Integer pageNum, Integer pageSize);

    /** 意见详情（自动标记已读） */
    FeedbackVO detail(Long id);

    /** 未读回复数（个人中心红点） */
    Long unreadCount();

    // ========== 管理端 ==========
    PageVO<AdminFeedbackVO> adminPage(FeedbackQueryDTO dto);

    AdminFeedbackVO adminDetail(Long id);

    void reply(Long id, FeedbackReplyDTO dto);

    void delete(Long id);

    /** 更新意见状态 */
    void updateStatus(Long id, Integer status);
}
