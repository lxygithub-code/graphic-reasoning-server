package com.lee.graphic_reasoning_server.vo;

import lombok.Data;

@Data
public class PracticeRecordVO {
    private Long id;
    private String examType;
    private String examTypeLabel;
    private Integer totalCount;
    private Integer correctCount;
    private Double accuracy;
    private Integer totalDuration;
    private String createTime;
}