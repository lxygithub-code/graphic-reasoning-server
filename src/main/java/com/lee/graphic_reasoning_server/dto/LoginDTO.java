package com.lee.graphic_reasoning_server.dto;

import lombok.Data;

@Data
public class LoginDTO {
    /** wx.login 返回的 code */
    private String code;
    /** 可选：用户信息（新版可省略） */
    private String nickname;
    private String avatarUrl;
    private Integer gender;
}