package com.lee.graphic_reasoning_server.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentQuestionVO {
    private Long questionId;
    private String content;          // 题目内容（截取 100 字）
    private String imageUrl;         // 题目图片
    private String category;
    private String categoryLabel;
    private String source;
    private Long commentCount;       // 该题评论数
    private LocalDateTime lastCommentTime;  // 最新评论时间
    private String lastCommentContent;      // 最新评论内容
}
