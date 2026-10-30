package com.lee.graphic_reasoning_server.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.lee.graphic_reasoning_server.common.BizException;
import com.lee.graphic_reasoning_server.config.OssConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OssServiceImpl implements OssService {

    private final OSS ossClient;
    private final OssConfig ossConfig;

    private static final List<String> ALLOW_EXT =
            Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "bmp");
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BizException("文件为空");
        if (file.getSize() > MAX_SIZE) throw new BizException("图片不能超过 5MB");

        String ext = FileUtil.extName(file.getOriginalFilename());
        if (StrUtil.isBlank(ext) || !ALLOW_EXT.contains(ext.toLowerCase())) {
            throw new BizException("只允许上传图片格式：" + ALLOW_EXT);
        }

        String objectKey = buildObjectKey(ext);
        try (InputStream is = file.getInputStream()) {
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(file.getSize());
            meta.setContentType(file.getContentType());
            ossClient.putObject(ossConfig.getBucketName(), objectKey, is, meta);
            return buildUrl(objectKey);
        } catch (IOException e) {
            log.error("OSS 上传失败", e);
            throw new BizException("图片上传失败：" + e.getMessage());
        }
    }

    @Override
    public String upload(File file, String contentType) {
        if (file == null || !file.exists()) return null;

        String ext = FileUtil.extName(file.getName());
        if (StrUtil.isBlank(ext) || !ALLOW_EXT.contains(ext.toLowerCase())) return null;

        String objectKey = buildObjectKey(ext);
        try (InputStream is = new FileInputStream(file)) {
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(file.length());
            meta.setContentType(StrUtil.blankToDefault(contentType, "image/png"));
            ossClient.putObject(ossConfig.getBucketName(), objectKey, is, meta);
            return buildUrl(objectKey);
        } catch (IOException e) {
            log.error("OSS 上传本地文件失败: {}", file.getAbsolutePath(), e);
            return null;
        }
    }

    private String buildObjectKey(String ext) {
        String datePath = DateUtil.format(new java.util.Date(), "yyyy/MM/dd/");
        String fileName = IdUtil.fastSimpleUUID() + "." + ext.toLowerCase();
        return ossConfig.getDirPrefix() + datePath + fileName;
    }

    private String buildUrl(String objectKey) {
        return ossConfig.getUrlPrefix() + "/" + objectKey;
    }


    @Override
    public String uploadBytes(byte[] data, String objectKey, String contentType) {
        if (data == null || data.length == 0) {
            throw new BizException("上传数据不能为空");
        }

        try (InputStream is = new ByteArrayInputStream(data)) {
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(data.length);
            meta.setContentType(contentType);

            // ★ 核心修改：处理中文文件名，强制浏览器识别正确的文件名
            // 判断如果是 pdf，则设置响应头
            if (objectKey.endsWith(".pdf")) {
                // 提取最后一段作为文件名
                String fileName = objectKey.substring(objectKey.lastIndexOf("/") + 1);
                try {
                    // 对中文名进行 URL 编码
                    String encodedFileName = java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
                    // 设置 Content-Disposition (attachment 表示作为附件下载/预览)
                    meta.setContentDisposition("attachment; filename=\"" + encodedFileName + "\"; filename*=utf-8''" + encodedFileName);
                } catch (Exception e) {
                    log.warn("PDF文件名编码失败", e);
                }
            }

            ossClient.putObject(ossConfig.getBucketName(), objectKey, is, meta);
            return buildUrl(objectKey);
        } catch (Exception e) {
            log.error("OSS 上传字节流失败: {}", objectKey, e);
            throw new BizException("文件上传失败");
        }
    }
}