package com.lee.graphic_reasoning_server.service;

import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.dto.DictQueryDTO;
import com.lee.graphic_reasoning_server.dto.DictSaveDTO;
import com.lee.graphic_reasoning_server.vo.DictTypeVO;
import com.lee.graphic_reasoning_server.vo.DictVO;

import java.util.List;

public interface DictService {

    /** 平铺列表（可选 level 过滤） */
    List<DictVO> listByType(String dictType, Integer level);

    /** 树形结构 */
    List<DictVO> treeByType(String dictType);

    /** 分页查询 */
    PageVO<DictVO> page(DictQueryDTO dto);

    /** 新增 */
    Long save(DictSaveDTO dto);

    /** 编辑 */
    void update(DictSaveDTO dto);

    /** 删除（连带删除子节点） */
    void delete(Long id);

    /** 所有字典类型（用于管理页下拉） */
    List<DictTypeVO> listTypes();

    /** 全量列表（管理页分组展示用） */
    List<DictVO> listAll();
}
