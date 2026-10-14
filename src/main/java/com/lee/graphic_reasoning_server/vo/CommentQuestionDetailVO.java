package com.lee.graphic_reasoning_server.vo;

import lombok.Data;

import java.util.List;

@Data
public class CommentQuestionDetailVO {
    // 题目信息
    private QuestionDetailVO question;
    // 评论列表
    private List<CommentItemVO> comments;
}