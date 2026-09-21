package com.lee.graphic_reasoning_server.vo;

import lombok.Data;

@Data
public class AdminLoginVO {
    private String token;
    private Long adminId;
    private String username;
    private String nickname;
    private String avatar;
}