package com.lee.graphic_reasoning_server.vo;

import lombok.Data;

import java.util.List;

@Data
public class PracticeSingleVO {
    private Boolean isCorrect;
    private String correctOption;
    private String analysis;
    private List<CommentVO> comments;
    /** 分平台解析列表 */
    private List<QuestionDetailVO.AnalysisVO> analyses;
}
