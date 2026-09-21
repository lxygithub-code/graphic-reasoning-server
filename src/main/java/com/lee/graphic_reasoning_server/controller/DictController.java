package com.lee.graphic_reasoning_server.controller;

import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.common.R;
import com.lee.graphic_reasoning_server.dto.DictQueryDTO;
import com.lee.graphic_reasoning_server.dto.DictSaveDTO;
import com.lee.graphic_reasoning_server.service.DictService;
import com.lee.graphic_reasoning_server.vo.DictTypeVO;
import com.lee.graphic_reasoning_server.vo.DictVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    /** 平铺列表，例如 /api/dict/list?dictType=question_category&level=2 */
    @GetMapping("/list")
    public R<List<DictVO>> list(@RequestParam String dictType,
                                @RequestParam(required = false) Integer level) {
        return R.ok(dictService.listByType(dictType, level));
    }

    /** 树形结构，例如 /api/dict/tree?dictType=question_category */
    @GetMapping("/tree")
    public R<List<DictVO>> tree(@RequestParam String dictType) {
        return R.ok(dictService.treeByType(dictType));
    }

    /** 所有字典类型 */
    @GetMapping("/types")
    public R<List<DictTypeVO>> types() {
        return R.ok(dictService.listTypes());
    }

    /** 分页查询 */
    @PostMapping("/page")
    public R<PageVO<DictVO>> page(@RequestBody DictQueryDTO dto) {
        return R.ok(dictService.page(dto));
    }

    /** 新增 */
    @PostMapping
    public R<Long> save(@Valid @RequestBody DictSaveDTO dto) {
        return R.ok(dictService.save(dto));
    }

    /** 编辑 */
    @PutMapping
    public R<Void> update(@Valid @RequestBody DictSaveDTO dto) {
        dictService.update(dto);
        return R.ok();
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        dictService.delete(id);
        return R.ok();
    }

    /** 全量列表（前端按 dictType 分组展示） */
    @GetMapping("/all")
    public R<List<DictVO>> all() {
        return R.ok(dictService.listAll());
    }
}
