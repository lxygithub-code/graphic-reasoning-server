package com.lee.graphic_reasoning_server.dto;

import lombok.Data;

@Data
public class CommentQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String keyword;          // 评论内容 / 题目内容 关键词
    private Integer status;          // 评论状态过滤（可选）
    private String startDate;        // 起始日期
    private String endDate;
}