package com.lee.graphic_reasoning_server.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AdminMessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long adminId;
    private String type;
    private String title;
    private String content;
    private Long refId;
    private Long refQuestionId;
    private Integer isRead;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}