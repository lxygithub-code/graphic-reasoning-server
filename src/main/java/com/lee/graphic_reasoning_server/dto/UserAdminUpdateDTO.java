package com.lee.graphic_reasoning_server.dto;

import lombok.Data;

@Data
public class UserAdminUpdateDTO {
    private Long userId;
    /** 是否允许评论 */
    private Integer canComment;
    /** 是否展示评论 */
    private Integer showComment;
    /** 用户状态（启用/禁用） */
    private Integer status;
}