package com.lee.graphic_reasoning_server.vo;

import com.lee.graphic_reasoning_server.po.Question;
import lombok.Data;

import java.util.List;

@Data
public class QuestionPracticeVO {
    private Long id;
    private String content;
    private List<Question.Option> options;
    private String source;
    private String category;
    private Integer difficulty;
    private String examType;
    private String examSubType;
    private String imageUrl;
    /** 用户对该题的作答状态：null 未答 / 1 曾答对 / 0 曾答错 */
    private Integer userLastCorrect;
}