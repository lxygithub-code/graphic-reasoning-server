package com.lee.graphic_reasoning_server.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserRetentionVO implements Serializable {
    private Long onceCount = 0L;     // 仅活跃一次的用户数
    private Long multipleCount = 0L; // 活跃多次的用户数
}