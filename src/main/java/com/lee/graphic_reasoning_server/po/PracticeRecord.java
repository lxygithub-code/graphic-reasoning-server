package com.lee.graphic_reasoning_server.po;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_practice_record")
public class PracticeRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 id */
    private Long userId;

    /** 练习类型，如 图形推理 */
    private String category;

    /** 题目总数 */
    private Integer totalCount;

    /** 答对数量 */
    private Integer correctCount;

    /** 正确率 % */
    private BigDecimal accuracy;

    /** 总耗时（秒） */
    private Integer totalDuration;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
