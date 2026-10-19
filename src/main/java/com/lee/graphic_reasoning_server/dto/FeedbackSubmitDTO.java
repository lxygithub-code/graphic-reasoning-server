package com.lee.graphic_reasoning_server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FeedbackSubmitDTO {

    @NotBlank(message = "意见内容不能为空")
    @Size(max = 1000, message = "意见内容不超过 1000 字")
    private String content;

}