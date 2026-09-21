package com.lee.graphic_reasoning_server.vo;

import lombok.Data;

@Data
public class CommentVO {
    private Long id;
    private String nickname;
    private String content;
    private Integer likeCount;
    private String createTime;
}
