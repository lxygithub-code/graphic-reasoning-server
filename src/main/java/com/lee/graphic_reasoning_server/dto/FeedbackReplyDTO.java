package com.lee.graphic_reasoning_server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FeedbackReplyDTO {

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 1000, message = "回复内容不超过 1000 字")
    private String replyContent;
}
