package com.lee.graphic_reasoning_server.po;

import com.baomidou.mybatisplus.annotation.*;
import com.lee.graphic_reasoning_server.handler.QuestionOptionListTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "t_question", autoResultMap = true)
public class Question implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 题目内容 */
    private String content;

    /** 选项，JSON 存储 */
    @TableField(typeHandler = QuestionOptionListTypeHandler.class)
    private List<Option> options;

    /** 正确选项，如 "A" */
    private String correctOption;

    /** 题目来源 */
    private String source;

    /** 所属类型，如 "图形推理" */
    private String category;

    /** 考试类型编码：guokao/shengkao/shiye */
    private String examType;

    /** 难度 1-5 */
    private Integer difficulty;

    // 题目图片 URL
    private String imageUrl;

    /** 解析 */
    private String analysis;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;

    @Data
    public static class Option implements Serializable {
        /** 选项 key：A / B / C / D */
        private String key;

        /** 选项类型：text（文字） / image（图片） */
        private String type = "text";

        /**
         * 选项内容：
         * - type = text  时，value 是选项文字
         * - type = image 时，value 是图片 URL
         */
        private String value;
    }
}
