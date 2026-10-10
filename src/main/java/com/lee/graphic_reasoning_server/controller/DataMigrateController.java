package com.lee.graphic_reasoning_server.controller;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lee.graphic_reasoning_server.common.R;
import com.lee.graphic_reasoning_server.mapper.DictMapper;
import com.lee.graphic_reasoning_server.po.Dict;
import com.lee.graphic_reasoning_server.po.Question;
import com.lee.graphic_reasoning_server.po.QuestionAnalysis;
import com.lee.graphic_reasoning_server.po.User;
import com.lee.graphic_reasoning_server.mapper.QuestionAnalysisMapper;
import com.lee.graphic_reasoning_server.mapper.QuestionMapper;
import com.lee.graphic_reasoning_server.mapper.UserMapper;
import com.lee.graphic_reasoning_server.service.OssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/migrate")
@RequiredArgsConstructor
public class DataMigrateController {

    private final OssService ossService;
    private final QuestionMapper questionMapper;
    private final QuestionAnalysisMapper analysisMapper;
    private final UserMapper userMapper;
    private final DictMapper dictMapper;

    /** 本地旧图片根目录（跟原 file.upload.path 一致） */
    @Value("${file.upload.path:/opt/upload/}")
    private String localUploadPath;

    private final Map<String, String> cache = new HashMap<>();

    @PostMapping("/upload-to-oss")
    public R<Map<String, Object>> migrateAll() {
        int qCount = 0, optCount = 0, anaCount = 0, userCount = 0;

        // 1. 题目主图
        List<Question> questions = questionMapper.selectList(null);
        for (Question q : questions) {
            boolean changed = false;

            if (needMigrate(q.getImageUrl())) {
                String newUrl = migrateOne(q.getImageUrl());
                if (newUrl != null) { q.setImageUrl(newUrl); qCount++; changed = true; }
            }

            // 2. 选项图片
            if (q.getOptions() != null) {
                for (Question.Option opt : q.getOptions()) {
                    if ("image".equals(opt.getType()) && needMigrate(opt.getValue())) {
                        String newUrl = migrateOne(opt.getValue());
                        if (newUrl != null) { opt.setValue(newUrl); optCount++; changed = true; }
                    }
                }
            }

            if (changed) questionMapper.updateById(q);
        }

        // 3. 平台解析图片
        List<QuestionAnalysis> analyses = analysisMapper.selectList(null);
        for (QuestionAnalysis a : analyses) {
            if ("image".equals(a.getType()) && needMigrate(a.getContent())) {
                String newUrl = migrateOne(a.getContent());
                if (newUrl != null) {
                    a.setContent(newUrl);
                    analysisMapper.updateById(a);
                    anaCount++;
                }
            }
        }

        // 4. 用户头像
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .isNotNull(User::getAvatarUrl).ne(User::getAvatarUrl, ""));
        for (User u : users) {
            if (needMigrate(u.getAvatarUrl())) {
                String newUrl = migrateOne(u.getAvatarUrl());
                if (newUrl != null) {
                    u.setAvatarUrl(newUrl);
                    userMapper.updateById(u);
                    userCount++;
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("题目主图", qCount);
        result.put("选项图片", optCount);
        result.put("平台解析图", anaCount);
        result.put("用户头像", userCount);
        return R.ok(result);
    }

    /** 判断是否需要迁移 */
    private boolean needMigrate(String url) {
        return StrUtil.isNotBlank(url) && url.startsWith("/upload/");
    }

    /** 迁移单个：/upload/xxx → OSS URL */
    private String migrateOne(String localUrl) {
        if (cache.containsKey(localUrl)) return cache.get(localUrl);

        try {
            String relative = localUrl.replaceFirst("^/upload/", "");
            File localFile = new File(localUploadPath + relative);
            if (!localFile.exists()) {
                log.warn("本地文件不存在，跳过: {}", localFile.getAbsolutePath());
                cache.put(localUrl, null);
                return null;
            }

            String newUrl = ossService.upload(localFile, null);
            cache.put(localUrl, newUrl);
            log.info("迁移成功: {} → {}", localUrl, newUrl);
            return newUrl;
        } catch (Exception e) {
            log.error("迁移失败: {}", localUrl, e);
            cache.put(localUrl, null);
            return null;
        }
    }


    @PostMapping("/category-label-to-value")
    public R<Map<String, Object>> migrateCategoryToValue() {
        return R.ok(doMigrateCategoryToValue());
    }

    private Map<String, Object> doMigrateCategoryToValue() {
        // 1. 拉所有 question_category 字典（含 status=1）
        List<Dict> all = dictMapper.selectList(
                new LambdaQueryWrapper<Dict>()
                        .eq(Dict::getDictType, "question_category")
                        .eq(Dict::getStatus, 1));
        // 按 parentId 分组
        Map<Long, List<Dict>> byParent = all.stream()
                .collect(Collectors.groupingBy(Dict::getParentId));

        // 2. 递归构建 label路径 → value路径 的映射
        Map<String, String> labelToValue = new HashMap<>();
        buildPathMap(byParent, 0L, "", "", labelToValue);

        // 3. 找出所有需要迁移的题目
        List<Question> questions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .isNotNull(Question::getCategory)
                        .ne(Question::getCategory, ""));

        int migrated = 0, skipped = 0;
        List<String> failed = new ArrayList<>();

        for (Question q : questions) {
            String oldPath = q.getCategory();
            // 已经是 value 路径的（包含下划线）跳过
            if (oldPath.matches("^[a-z_]+(/[a-z_]+)*$")) {
                skipped++;
                continue;
            }
            String newPath = labelToValue.get(oldPath);
            if (newPath == null) {
                failed.add(oldPath);
                continue;
            }
            Question update = new Question();
            update.setId(q.getId());
            update.setCategory(newPath);
            questionMapper.updateById(update);
            migrated++;
        }

        Map<String, Object> res = new HashMap<>();
        res.put("migrated", migrated);
        res.put("skipped", skipped);
        res.put("failedCount", failed.size());
        res.put("failedPaths", failed);   // 返回没匹配上的，人工核对
        return res;
    }

    /**
     * 递归构建 label 路径 → value 路径
     * @param byParent   parentId → children
     * @param parentId   当前父节点 id
     * @param labelPrefix 父级 label 路径（如 "黑白块/一、黑白球..."）
     * @param valuePrefix 父级 value 路径（如 "black_white_block/bw_ball"）
     */
    private void buildPathMap(Map<Long, List<Dict>> byParent, Long parentId,
                              String labelPrefix, String valuePrefix,
                              Map<String, String> result) {
        List<Dict> children = byParent.getOrDefault(parentId, Collections.emptyList());
        for (Dict d : children) {
            String curLabelPath = StrUtil.isBlank(labelPrefix)
                    ? d.getDictLabel()
                    : labelPrefix + "/" + d.getDictLabel();
            String curValuePath = StrUtil.isBlank(valuePrefix)
                    ? d.getDictValue()
                    : valuePrefix + "/" + d.getDictValue();

            result.put(curLabelPath, curValuePath);
            // 递归子节点
            buildPathMap(byParent, d.getId(), curLabelPath, curValuePath, result);
        }
    }
}
