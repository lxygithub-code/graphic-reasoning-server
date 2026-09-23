package com.lee.graphic_reasoning_server.dto;

import lombok.Data;

@Data
public class AdminUpdateDTO {

    /** 新账号（可选修改） */
    private String username;

    /** 新昵称（可选修改） */
    private String nickname;

    /** 新头像（可选修改） */
    private String avatar;
}