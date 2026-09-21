package com.lee.graphic_reasoning_server.controller;

import com.lee.graphic_reasoning_server.common.R;
import com.lee.graphic_reasoning_server.service.OssService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/upload")
@RequiredArgsConstructor
public class UploadController {

    private final OssService ossService;

    @PostMapping("/image")
    public R<String> image(@RequestParam("file") MultipartFile file) {
        return R.ok(ossService.upload(file));
    }
}
