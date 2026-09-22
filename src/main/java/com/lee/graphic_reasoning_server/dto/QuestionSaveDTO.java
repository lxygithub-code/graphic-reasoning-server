package com.lee.graphic_reasoning_server.dto;

import com.lee.graphic_reasoning_server.po.Question;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class QuestionSaveDTO {

    private Long id;

    @NotBlank(message = "题目内容不能为空")
    private String content;

    @NotEmpty(message = "选项不能为空")
    private List<Question.Option> options;

    @NotBlank(message = "正确选项不能为空")
    private String correctOption;

    private String source;

    private String category;

    private String examType;

    private String examSubType;

    private Integer difficulty = 1;

    private String imageUrl;

    /** 分平台解析 */
    private List<AnalysisItem> analyses;

    @Data
    public static class AnalysisItem {
        private String platform;   // 平台编码
        /** 类型：text / image */
        private String type = "text";
        private String content;    // 解析内容
    }
}
