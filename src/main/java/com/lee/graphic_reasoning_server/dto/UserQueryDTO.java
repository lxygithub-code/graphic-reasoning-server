package com.lee.graphic_reasoning_server.dto;

import lombok.Data;

@Data
public class UserQueryDTO {
    private String nickname;
    private Integer status;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}