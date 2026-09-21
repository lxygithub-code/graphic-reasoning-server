package com.lee.graphic_reasoning_server.vo;

import com.lee.graphic_reasoning_server.po.Question;
import lombok.Data;

import java.util.List;

@Data
public class WrongQuestionDetailVO {

    // === 题目信息 ===
    private Long questionId;
    private String content;
    private String imageUrl;
    private List<Question.Option> options;
    private String correctOption;
    private String category;
    private String examType;
    private String source;
    private Integer difficulty;
    private String analysis;
    private List<QuestionDetailVO.AnalysisVO> analyses;

    // === 错题记录 ===
    private Integer wrongCount;         // 累计错误次数
    private String lastWrongTime;       // 最近一次错误时间

    // === 当时作答 ===
    private String userOption;          // 用户当时选的选项
    private Integer duration;           // 当时耗时（秒）
    private String answerTime;          // 当时作答时间
}