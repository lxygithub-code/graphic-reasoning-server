package com.lee.graphic_reasoning_server.controller;

import com.lee.graphic_reasoning_server.common.R;
import com.lee.graphic_reasoning_server.service.StatService;
import com.lee.graphic_reasoning_server.vo.StatVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class StatController {

    private final StatService statService;

    @GetMapping
    public R<StatVO> overview() {
        return R.ok(statService.overview());
    }
}