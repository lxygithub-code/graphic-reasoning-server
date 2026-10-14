package com.lee.graphic_reasoning_server.service;

import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.dto.CommentQueryDTO;
import com.lee.graphic_reasoning_server.vo.CommentQuestionDetailVO;
import com.lee.graphic_reasoning_server.vo.CommentQuestionVO;

public interface AdminCommentService {

    /** 有评论的题目列表（分页） */
    PageVO<CommentQuestionVO> questionPage(CommentQueryDTO dto);

    /** 某题目的评论详情（含题目 + 全部评论） */
    CommentQuestionDetailVO questionDetail(Long questionId);

    /** 隐藏 / 恢复评论 */
    void updateStatus(Long id, Integer status);

    /** 删除评论 */
    void delete(Long id);
}
