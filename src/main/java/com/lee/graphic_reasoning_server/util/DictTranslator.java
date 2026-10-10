package com.lee.graphic_reasoning_server.util;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lee.graphic_reasoning_server.mapper.DictMapper;
import com.lee.graphic_reasoning_server.po.Dict;
import com.lee.graphic_reasoning_server.vo.DictVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DictTranslator {

    private final DictMapper dictMapper;

    /**
     * value路径 → label路径 缓存
     */
    private volatile Map<String, String> valueToLabelCache;
    /**
     * label路径 → value路径 缓存
     */
    private volatile Map<String, String> labelToValueCache;

    public String valuePathToLabelPath(String valuePath) {
        if (StrUtil.isBlank(valuePath)) {
            return valuePath;
        }
        ensureCache();
        return valueToLabelCache.getOrDefault(valuePath, valuePath);
    }

    public String labelPathToValuePath(String labelPath) {
        if (StrUtil.isBlank(labelPath)) {
            return labelPath;
        }
        ensureCache();
        return labelToValueCache.getOrDefault(labelPath, labelPath);
    }

    /**
     * 供定时任务 / 手动刷新用
     */
    public synchronized void refresh() {
        // 直接用 mapper 查询
        List<Dict> all = dictMapper.selectList(
                new LambdaQueryWrapper<Dict>()
                        .eq(Dict::getDictType, "question_category")
                        .eq(Dict::getStatus, 1)
                        .orderByAsc(Dict::getSort)
                        .orderByAsc(Dict::getId));

        // 构建 parentId → children 的 map
        Map<Long, List<Dict>> byParent = all.stream()
                .collect(Collectors.groupingBy(Dict::getParentId));

        Map<String, String> v2l = new HashMap<>();
        Map<String, String> l2v = new HashMap<>();
        buildMap(byParent, 0L, "", "", v2l, l2v);

        this.valueToLabelCache = v2l;
        this.labelToValueCache = l2v;
        log.info("[DictTranslator] 缓存刷新完成，共 {} 条路径", v2l.size());
    }

    private void buildMap(Map<Long, List<Dict>> byParent, Long parentId,
                          String parentValue, String parentLabel,
                          Map<String, String> v2l, Map<String, String> l2v) {
        List<Dict> children = byParent.getOrDefault(parentId, Collections.emptyList());
        for (Dict d : children) {
            String vp = StrUtil.isBlank(parentValue)
                    ? d.getDictValue() : parentValue + "/" + d.getDictValue();
            String lp = StrUtil.isBlank(parentLabel)
                    ? d.getDictLabel() : parentLabel + "/" + d.getDictLabel();
            v2l.put(vp, lp);
            l2v.put(lp, vp);
            buildMap(byParent, d.getId(), vp, lp, v2l, l2v);
        }
    }

    private void ensureCache() {
        if (valueToLabelCache == null || labelToValueCache == null) {
            synchronized (this) {
                if (valueToLabelCache == null || labelToValueCache == null) {
                    refresh();
                }
            }
        }
    }


    /**
     * 批量 value路径 → label路径
     * 用于列表页一次翻译多行
     */
    public Map<String, String> batchValueToLabelPath(java.util.Collection<String> valuePaths) {
        Map<String, String> result = new HashMap<>();
        if (valuePaths == null || valuePaths.isEmpty()) {
            return result;
        }

        ensureCache();
        for (String vp : valuePaths) {
            if (StrUtil.isBlank(vp)) {
                continue;
            }
            result.put(vp, valueToLabelCache.getOrDefault(vp, vp));
        }
        return result;
    }

    /**
     * 单个翻译（内部用，方便）
     */
    public String valueToLabel(String valuePath) {
        return valuePathToLabelPath(valuePath);
    }
}
