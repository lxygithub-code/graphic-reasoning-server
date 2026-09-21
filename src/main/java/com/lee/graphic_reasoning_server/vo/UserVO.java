package com.lee.graphic_reasoning_server.vo;

import lombok.Data;

@Data
public class UserVO {
    private Long id;
    private String nickname;
    private String avatarUrl;
    private Integer gender;
    private String lastLoginTime;
}