package com.lee.graphic_reasoning_server.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssConfig {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private String urlPrefix;
    private String dirPrefix = "";

    private OSS ossClient;

    @Bean
    public OSS ossClient() {
        this.ossClient = new OSSClientBuilder()
                .build(endpoint, accessKeyId, accessKeySecret);
        return this.ossClient;
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) ossClient.shutdown();
    }
}
