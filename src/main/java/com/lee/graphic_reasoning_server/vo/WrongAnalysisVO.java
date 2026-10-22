package com.lee.graphic_reasoning_server.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class WrongAnalysisVO {
    private List<Map<String, Object>> categoryStats;   // 错题类型分布 [{name: "黑白块", value: 100}]
    private List<Map<String, Object>> topWrongQuestions; // 错题 TOP 10
}