package com.lee.graphic_reasoning_server.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_favorite")
public class Favorite implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long questionId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}