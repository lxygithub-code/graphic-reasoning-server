package com.lee.graphic_reasoning_server.controller;

import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.common.R;
import com.lee.graphic_reasoning_server.dto.QuestionQueryDTO;
import com.lee.graphic_reasoning_server.dto.QuestionSaveDTO;
import com.lee.graphic_reasoning_server.po.Question;
import com.lee.graphic_reasoning_server.service.QuestionService;
import com.lee.graphic_reasoning_server.vo.QuestionDetailVO;
import com.lee.graphic_reasoning_server.vo.QuestionSourceStatVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/question")
@RequiredArgsConstructor
public class QuestionAdminController {

    private final QuestionService questionService;

    @PostMapping("/page")
    public R<PageVO<QuestionDetailVO>> page(@RequestBody QuestionQueryDTO dto) {
        return R.ok(questionService.page(dto));
    }

    @PostMapping
    public R<Long> save(@Valid @RequestBody QuestionSaveDTO dto) {
        return R.ok(questionService.save(dto));
    }

    @PutMapping
    public R<Void> update(@Valid @RequestBody QuestionSaveDTO dto) {
        questionService.update(dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return R.ok();
    }

    @DeleteMapping("/batch")
    public R<Void> batchDelete(@RequestBody List<Long> ids) {
        questionService.batchDelete(ids);
        return R.ok();
    }

    @GetMapping("/{id}")
    public R<QuestionDetailVO> detail(@PathVariable Long id) {
        return R.ok(questionService.detail(id));
    }

    /** 套卷列表（含题目数量） */
    @GetMapping("/sources")
    public R<List<QuestionSourceStatVO>> sources(
            @RequestParam(required = false) String examType,
            @RequestParam(required = false) String examSubType,
            @RequestParam(required = false) String keyword) {
        return R.ok(questionService.sourceStats(examType, examSubType, keyword));
    }

    /** 某套卷下的所有题目 */
    @GetMapping("/by-source")
    public R<List<QuestionDetailVO>> bySource(
            @RequestParam(required = false, defaultValue = "") String source) {
        return R.ok(questionService.listBySource(source));
    }
}
