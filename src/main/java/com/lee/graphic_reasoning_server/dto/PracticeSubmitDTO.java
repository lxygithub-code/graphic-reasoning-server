package com.lee.graphic_reasoning_server.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class PracticeSubmitDTO {

    private String examType;

    /** 本次练习的题目 id 顺序 */
    private List<Long> questionIds;

    /** 总耗时（秒） */
    private Integer totalTime = 0;

    /** 每题作答 */
    @NotEmpty(message = "answers 不能为空")
    private List<Item> answers;

    @Data
    public static class Item {
        private Long questionId;
        private String userAnswer;
        private Integer timeSpent;
    }
}