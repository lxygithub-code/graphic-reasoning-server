package com.lee.graphic_reasoning_server.dto;

import lombok.Data;

@Data
public class QuestionRandomDTO {
    private String category;
    private String examType;
    private Integer count = 10;

    /** 三种题目的抽取权重（%，三者之和必须 100，否则按默认 60/20/20） */
    private Integer weightUnknown = 60;
    private Integer weightCorrect = 20;
    private Integer weightWrong = 20;
}
