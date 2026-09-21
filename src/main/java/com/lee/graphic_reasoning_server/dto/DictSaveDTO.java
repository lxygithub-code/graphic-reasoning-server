package com.lee.graphic_reasoning_server.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DictSaveDTO {

    private Long id;

    @NotBlank(message = "字典类型不能为空")
    private String dictType;

    @NotBlank(message = "显示名称不能为空")
    private String dictLabel;

    @NotBlank(message = "存储值不能为空")
    private String dictValue;

    /** 父节点 id，0 表示顶级 */
    private Long parentId = 0L;

    /** 层级：1 一级，2 二级 */
    private Integer level = 1;

    private Integer sort = 0;

    private Integer status = 1;

    private String remark;
}
