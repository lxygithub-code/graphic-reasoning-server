package com.lee.graphic_reasoning_server.vo;

import com.lee.graphic_reasoning_server.po.Question;
import lombok.Data;

import java.util.List;

@Data
public class QuestionDetailVO {
    private Long id;
    private String content;
    private List<Question.Option> options;
    private String correctOption;
    private String source;
    private String category;
    private Integer difficulty;
    private String examType;
    private String examSubType;
    private String imageUrl;

    /** 分平台解析 */
    private List<AnalysisVO> analyses;

    @Data
    public static class AnalysisVO {
        private String platform;    // 平台编码
        private String platformLabel; // 平台中文名（可选，后端补）
        private String type;
        private String content;
    }
}