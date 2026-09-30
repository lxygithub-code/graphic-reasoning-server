package com.lee.graphic_reasoning_server.vo;

import lombok.Data;

@Data
public class QuestionSourceStatVO {
    private String source;
    private Long count;
    private String examType;
    private String examSubType;
    /** 可抽取的题目数量 */
    private Integer extractableCount;
}