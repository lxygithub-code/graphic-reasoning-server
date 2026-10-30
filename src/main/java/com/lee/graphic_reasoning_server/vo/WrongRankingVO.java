package com.lee.graphic_reasoning_server.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class WrongRankingVO implements Serializable {
    private Long questionId;
    private String content;
    private String categoryLabel;
    private Long wrongCount;
}