package com.lee.graphic_reasoning_server.controller;

import com.lee.graphic_reasoning_server.common.R;
import com.lee.graphic_reasoning_server.dto.QuestionRandomDTO;
import com.lee.graphic_reasoning_server.service.QuestionService;
import com.lee.graphic_reasoning_server.vo.ExamTypeCountVO;
import com.lee.graphic_reasoning_server.vo.QuestionDetailVO;
import com.lee.graphic_reasoning_server.vo.QuestionPracticeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/question")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    /** 随机抽题 */
    @PostMapping("/random")
    public R<List<QuestionPracticeVO>> random(@RequestBody QuestionRandomDTO dto) {
        return R.ok(questionService.randomPractice(dto));
    }

    /** 题目详情（含答案） */
    @GetMapping("/{id}")
    public R<QuestionDetailVO> detail(@PathVariable Long id) {
        return R.ok(questionService.detail(id));
    }

    /** 按考试类型统计题目数量 */
    @GetMapping("/count-by-exam-type")
    public R<List<ExamTypeCountVO>> countByExamType() {
        return R.ok(questionService.countByExamType());
    }
}