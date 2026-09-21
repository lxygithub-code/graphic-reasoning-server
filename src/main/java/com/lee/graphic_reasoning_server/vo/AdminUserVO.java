package com.lee.graphic_reasoning_server.vo;

import lombok.Data;

@Data
public class AdminUserVO {
    private Long id;
    private String nickname;
    private String avatarUrl;
    private Integer gender;
    private Integer status;
    private String lastLoginTime;
    private String createTime;
}