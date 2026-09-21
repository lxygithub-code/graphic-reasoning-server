package com.lee.graphic_reasoning_server.vo;

import lombok.Data;

import java.util.List;

@Data
public class PracticeRecordDetailVO {
    private Long recordId;
    private String examType;
    private Integer totalCount;
    private Integer correctCount;
    private Double accuracy;
    private Integer totalDuration;
    private String createTime;
    private List<PracticeDetailItemVO> items;
}