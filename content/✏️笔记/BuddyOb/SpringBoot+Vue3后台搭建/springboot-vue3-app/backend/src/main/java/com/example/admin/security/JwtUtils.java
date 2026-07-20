package com.example.admin.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具（jjwt 0.12 API）
 * - 签发：Jwts.builder().signWith(key, HS256)
 * - 解析：Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
 * - key 必须是 SecretKey，且长度 ≥ 256 bit（32 字节）
 */
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-minutes:720}")
    private long expirationMinutes;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 生成 token（subject = 用户名） */
    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMinutes * 60L * 1000L);
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(exp)
                .signWith(getKey(), Jwts.SIG.HS256)
                .compact();
    }

    /** 解析 token → Claims（签名/过期错误由调用方捕获） */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getBody();
    }

    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }
}
