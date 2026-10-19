package com.lee.graphic_reasoning_server.po;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_feedback")
public class Feedback implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String content;

    /** 0待处理 1处理中 2已解决 */
    private Integer status;

    private String replyContent;

    private Long replyAdminId;

    private String replyAdminName;

    private LocalDateTime replyTime;

    /** 0未读 1已读 */
    private Integer userRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;
}