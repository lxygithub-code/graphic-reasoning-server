package com.lee.graphic_reasoning_server.dto;

import lombok.Data;

@Data
public class QuestionQueryDTO {
    private String category;
    private String source;
    private Integer difficulty;
    private String keyword;
    /** 考试类型筛选 */
    private String examType;
    private String examSubType;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
