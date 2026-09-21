package com.lee.graphic_reasoning_server.service;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lee.graphic_reasoning_server.common.BizException;
import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.dto.DictQueryDTO;
import com.lee.graphic_reasoning_server.dto.DictSaveDTO;
import com.lee.graphic_reasoning_server.mapper.DictMapper;
import com.lee.graphic_reasoning_server.po.Dict;
import com.lee.graphic_reasoning_server.vo.DictTypeVO;
import com.lee.graphic_reasoning_server.vo.DictVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DictServiceImpl implements DictService {

    private final DictMapper dictMapper;

    @Override
    public List<DictVO> listByType(String dictType, Integer level) {
        LambdaQueryWrapper<Dict> qw = new LambdaQueryWrapper<Dict>()
                .eq(Dict::getDictType, dictType)
                .eq(Dict::getStatus, 1)
                .eq(level != null, Dict::getLevel, level)
                .orderByAsc(Dict::getLevel)
                .orderByAsc(Dict::getSort)
                .orderByAsc(Dict::getId);

        return dictMapper.selectList(qw).stream()
                .map(d -> {
                    DictVO vo = new DictVO();
                    BeanUtil.copyProperties(d, vo);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<DictTypeVO> listTypes() {
        return dictMapper.selectTypes();
    }

    @Override
    public List<DictVO> treeByType(String dictType) {
        List<Dict> all = dictMapper.selectList(
                new LambdaQueryWrapper<Dict>()
                        .eq(Dict::getDictType, dictType)
                        .eq(Dict::getStatus, 1)
                        .orderByAsc(Dict::getSort)
                        .orderByAsc(Dict::getId));

        List<DictVO> vos = all.stream().map(d -> {
            DictVO vo = new DictVO();
            BeanUtil.copyProperties(d, vo);
            return vo;
        }).toList();

        Map<Long, DictVO> idMap = vos.stream()
                .collect(Collectors.toMap(DictVO::getId, v -> v));

        List<DictVO> roots = new ArrayList<>();
        for (DictVO vo : vos) {
            if (vo.getParentId() == null || vo.getParentId() == 0) {
                roots.add(vo);
            } else {
                DictVO parent = idMap.get(vo.getParentId());
                if (parent != null) {
                    parent.getChildren().add(vo);
                } else {
                    roots.add(vo);   // 父级被禁用/删除时降级为顶级
                }
            }
        }
        return roots;
    }

    @Override
    public PageVO<DictVO> page(DictQueryDTO dto) {
        LambdaQueryWrapper<Dict> qw = new LambdaQueryWrapper<Dict>()
                .eq(StrUtil.isNotBlank(dto.getDictType()), Dict::getDictType, dto.getDictType())
                .eq(dto.getStatus() != null, Dict::getStatus, dto.getStatus())
                .and(StrUtil.isNotBlank(dto.getKeyword()), w -> w
                        .like(Dict::getDictLabel, dto.getKeyword())
                        .or()
                        .like(Dict::getDictValue, dto.getKeyword()))
                .orderByAsc(Dict::getLevel)
                .orderByAsc(Dict::getSort)
                .orderByAsc(Dict::getId);

        Page<Dict> page = dictMapper.selectPage(
                new Page<>(dto.getPageNum(), dto.getPageSize()), qw);

        List<DictVO> records = page.getRecords().stream().map(d -> {
            DictVO vo = new DictVO();
            BeanUtil.copyProperties(d, vo);
            return vo;
        }).collect(Collectors.toList());

        return PageVO.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    @Transactional
    public Long save(DictSaveDTO dto) {
        // 同一 dictType 下，value 不能重复
        Long count = dictMapper.selectCount(new LambdaQueryWrapper<Dict>()
                .eq(Dict::getDictType, dto.getDictType())
                .eq(Dict::getDictValue, dto.getDictValue()));
        if (count != null && count > 0) {
            throw new BizException("同一字典类型下，存储值「" + dto.getDictValue() + "」已存在");
        }

        Dict d = new Dict();
        BeanUtil.copyProperties(dto, d, "id");
        if (d.getParentId() == null) d.setParentId(0L);
        if (d.getLevel() == null) d.setLevel(1);
        if (d.getSort() == null) d.setSort(0);
        if (d.getStatus() == null) d.setStatus(1);

        // 如果有 parentId，自动推断 level
        if (d.getParentId() != null && d.getParentId() > 0) {
            Dict parent = dictMapper.selectById(d.getParentId());
            if (parent != null) {
                d.setLevel(parent.getLevel() + 1);
            }
        }
        dictMapper.insert(d);
        return d.getId();
    }

    @Override
    @Transactional
    public void update(DictSaveDTO dto) {
        if (dto.getId() == null) throw new BizException("id 不能为空");
        Dict exist = dictMapper.selectById(dto.getId());
        if (exist == null) throw new BizException("字典不存在");

        // 若修改 dictType + dictValue，检查重复
        if (!exist.getDictValue().equals(dto.getDictValue())
                || !exist.getDictType().equals(dto.getDictType())) {
            Long count = dictMapper.selectCount(new LambdaQueryWrapper<Dict>()
                    .eq(Dict::getDictType, dto.getDictType())
                    .eq(Dict::getDictValue, dto.getDictValue())
                    .ne(Dict::getId, dto.getId()));
            if (count != null && count > 0) {
                throw new BizException("同一字典类型下，存储值「" + dto.getDictValue() + "」已存在");
            }
        }

        Dict d = new Dict();
        BeanUtil.copyProperties(dto, d);

        // 自动算 level
        if (d.getParentId() != null && d.getParentId() > 0) {
            Dict parent = dictMapper.selectById(d.getParentId());
            if (parent != null) d.setLevel(parent.getLevel() + 1);
        } else {
            d.setParentId(0L);
            d.setLevel(1);
        }

        dictMapper.updateById(d);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // 递归删除所有子节点
        deleteRecursive(id);
    }

    private void deleteRecursive(Long id) {
        List<Dict> children = dictMapper.selectList(
                new LambdaQueryWrapper<Dict>().eq(Dict::getParentId, id));
        for (Dict child : children) {
            deleteRecursive(child.getId());
        }
        dictMapper.deleteById(id);
    }

    @Override
    public List<DictVO> listAll() {
        List<Dict> all = dictMapper.selectList(
                new LambdaQueryWrapper<Dict>()
                        .orderByAsc(Dict::getDictType)
                        .orderByAsc(Dict::getLevel)
                        .orderByAsc(Dict::getSort)
                        .orderByAsc(Dict::getId));

        return all.stream().map(d -> {
            DictVO vo = new DictVO();
            BeanUtil.copyProperties(d, vo);
            return vo;
        }).collect(Collectors.toList());
    }
}
