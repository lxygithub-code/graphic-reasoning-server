package com.lee.graphic_reasoning_server.po;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 微信 openid */
    private String openid;

    /** 微信 unionid（同一开放平台下唯一，可空） */
    private String unionid;

    private String nickname;

    private String avatarUrl;

    /** 0 未知 1 男 2 女 */
    private Integer gender;

    private LocalDateTime lastLoginTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;

    /** 1 启用 0 禁用 */
    private Integer status;

    /** 是否允许评论：0禁用 1允许 */
    private Integer canComment;

    /** 是否展示评论内容：0隐藏 1展示 */
    private Integer showComment;
}
