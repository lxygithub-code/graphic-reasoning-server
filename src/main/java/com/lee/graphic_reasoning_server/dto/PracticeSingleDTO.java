package com.lee.graphic_reasoning_server.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PracticeSingleDTO {

    @NotNull(message = "questionId 不能为空")
    private Long questionId;

    /** 用户答案，如 A/B/C/D，可为空（跳过） */
    private String userAnswer;

    /** 本题耗时（秒） */
    private Integer timeSpent = 0;

    /** 0 刷题 1 背题 */
    private Integer practiceMode = 1;
}