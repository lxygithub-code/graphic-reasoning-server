package com.lee.graphic_reasoning_server.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryTreeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String dictValue;
    private String dictLabel;

    /**
     * value 路径（用于抽题 / 匹配 t_question.category）
     * 例："black_white_block/bw_ball/bw_ball_single_group"
     */
    private String path;

    /**
     * label 路径（用于前端展示）
     * 例："黑白块/一、黑白球（黑白规则图形，外框相同）/1. 一组图 / 两组图"
     */
    private String labelPath;

    /** 该分类（含所有子类）的题目总数 */
    private Long count;

    private List<CategoryTreeVO> children = new ArrayList<>();
}
