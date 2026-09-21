package com.lee.graphic_reasoning_server.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtil {

    private static final String SECRET = "grs-please-change-this-secret-key-32bytes!!";
    private static final long EXPIRE_MS = 30L * 24 * 3600 * 1000;

    private static final SecretKey KEY =
            Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    public static String create(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId)) // 使用新方法
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRE_MS))
                .signWith(KEY) // 算法由 Key 类型自动推断
                .compact();
    }

    // parse 方法保持不变
    public static Long parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(KEY) // 使用 verifyWith 设置密钥
                    .build()
                    .parseSignedClaims(token) // 使用 parseSignedClaims 解析
                    .getPayload(); // 使用 getPayload 获取载荷

            return Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }
}