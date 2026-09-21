package com.lee.graphic_reasoning_server.controller;

import com.lee.graphic_reasoning_server.common.R;
import com.lee.graphic_reasoning_server.service.StatService;
import com.lee.graphic_reasoning_server.vo.StatVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class StatController {

    private final StatService statService;

    @GetMapping
    public R<StatVO> overview() {
        return R.ok(statService.overview());
    }


    /** ★ 增量趋势：近 N 天用户/题目/练习增量 */
    @GetMapping("/growth")
    public R<Map<String, Object>> growth(@RequestParam(defaultValue = "7") Integer days) {
        return R.ok(statService.growth(days));
    }

    /** ★ 活跃统计：近 N 天活跃用户 + 刷题量 */
    @GetMapping("/active")
    public R<Map<String, Object>> active(@RequestParam(defaultValue = "7") Integer days) {
        return R.ok(statService.active(days));
    }

    /** ★ 四大类题目数量分布 */
    @GetMapping("/category")
    public R<List<Map<String, Object>>> category() {
        return R.ok(statService.categoryDistribution());
    }
}