package com.lee.graphic_reasoning_server.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class FeedbackVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String content;
    private Integer status;
    /** 状态中文名 */
    private String statusLabel;
    private String replyContent;
    private String replyAdminName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime replyTime;

    private Integer userRead;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}