package com.lee.graphic_reasoning_server.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_practice_detail")
public class PracticeDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 t_practice_record.id */
    private Long recordId;

    private Long userId;

    private Long questionId;

    /** 用户当时选的选项 */
    private String userOption;

    /** 当时的正确答案（快照） */
    private String correctOption;

    /** 0 错 1 对 */
    private Integer isCorrect;

    /** 本题耗时（秒） */
    private Integer duration;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}