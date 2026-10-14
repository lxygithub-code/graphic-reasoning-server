package com.lee.graphic_reasoning_server.po;


import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_admin_message")
public class AdminMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 管理员ID，NULL = 群发给所有管理员 */
    private Long adminId;

    /** 消息类型：comment / question / system */
    private String type;

    /** 消息标题 */
    private String title;

    /** 摘要内容 */
    private String content;

    /** 关联业务ID（如评论ID） */
    private Long refId;

    /** 关联题目ID（跳转用） */
    private Long refQuestionId;

    /** 0 未读 1 已读 */
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
