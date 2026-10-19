package com.lee.graphic_reasoning_server.dto;

import lombok.Data;

@Data
public class FeedbackQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;

    /** 状态：0待处理 1已回复 null全部 */
    private Integer status;

    /** 关键词（意见内容 / 用户昵称） */
    private String keyword;
}
