package com.lee.graphic_reasoning_server.vo;

import com.lee.graphic_reasoning_server.po.Question;
import lombok.Data;

import java.util.List;

@Data
public class PracticeDetailItemVO {
    private Long questionId;
    private String content;              // 题干文字
    private String imageUrl;             // 题干图片
    private List<Question.Option> options; // 选项
    private String userOption;           // 用户选的
    private String correctOption;        // 正确答案
    private Boolean isCorrect;           // 是否答对
    private Integer duration;            // 本题耗时（秒）
    private String analysis;             // 解析（可选返回）
    private List<QuestionDetailVO.AnalysisVO> analyses;
}