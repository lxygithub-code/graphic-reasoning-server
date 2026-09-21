package com.lee.graphic_reasoning_server.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    /** 是否首次登录（新建用户），前端可用来引导完善资料 */
    private Boolean newUser;
}