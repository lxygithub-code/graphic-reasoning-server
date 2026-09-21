package com.lee.graphic_reasoning_server.dto;

import lombok.Data;

@Data
public class DictQueryDTO {
    private String dictType;
    private String keyword;   // 对 dictLabel / dictValue 模糊搜索
    private Integer status;
    private Integer pageNum = 1;
    private Integer pageSize = 50;
}