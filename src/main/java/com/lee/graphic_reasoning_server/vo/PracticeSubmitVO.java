package com.lee.graphic_reasoning_server.vo;

import lombok.Data;

@Data
public class PracticeSubmitVO {
    private Long recordId;
    private Integer totalCount;
    private Integer correctCount;
    private Double accuracy;    // 正确率 %
    private Integer totalTime;  // 总耗时（秒）
}
