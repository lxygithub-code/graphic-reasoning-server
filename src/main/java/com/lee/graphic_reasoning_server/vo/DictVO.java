package com.lee.graphic_reasoning_server.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DictVO {
    private Long id;
    private String dictType;
    private String dictLabel;
    private String dictValue;
    private Long parentId;
    private Integer level;
    private Integer sort;
    private Integer status;
    private String remark;
    private List<DictVO> children = new ArrayList<>();
}
