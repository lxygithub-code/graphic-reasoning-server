package com.lee.graphic_reasoning_server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentSaveDTO {

    @NotNull(message = "questionId 不能为空")
    private Long questionId;

    @NotBlank(message = "评论内容不能为空")
    private String content;
}
