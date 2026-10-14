package com.lee.graphic_reasoning_server.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentItemVO {
    private Long id;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String content;
    private Integer likeCount;
    private Integer status;
    private LocalDateTime createTime;
}
