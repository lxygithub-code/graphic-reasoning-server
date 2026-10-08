package com.lee.graphic_reasoning_server.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class QuestionDifficultyStatVO implements Serializable {
    private Long questionId;
    /** 累计作答次数 */
    private Long totalCount;
    /** 累计答对次数 */
    private Long correctCount;
}