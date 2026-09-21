package com.lee.graphic_reasoning_server.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface OssService {
    /** 上传 MultipartFile，返回完整 URL */
    String upload(MultipartFile file);

    /** 上传本地 File，返回完整 URL（供迁移工具用） */
    String upload(File file, String contentType);
}
