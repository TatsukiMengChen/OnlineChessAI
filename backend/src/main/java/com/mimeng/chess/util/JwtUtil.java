package com.mimeng.chess.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
  private static final long EXPIRATION = 1000 * 60 * 60 * 24; // 24小时
  private static SecretKey staticSecretKey;

  @Value("${jwt.secret.key}")
  private String secretKey;

  @PostConstruct
  private void init() {
    // 使用HMAC-SHA256算法创建密钥
    staticSecretKey = Keys.hmacShaKeyFor(secretKey.getBytes());
  }

  public static String generateToken(String userId, String email) {
    if (staticSecretKey == null) {
      throw new IllegalStateException("JWT secret key not initialized");
    }
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    claims.put("email", email);
    return Jwts.builder()
        .setClaims(claims)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
        .signWith(staticSecretKey, SignatureAlgorithm.HS256)
        .compact();
  }

  public static Claims parseToken(String token) {
    if (staticSecretKey == null) {
      throw new IllegalStateException("JWT secret key not initialized");
    }
    return Jwts.parserBuilder()
        .setSigningKey(staticSecretKey)
        .build()
        .parseClaimsJws(token)
        .getBody();
  }
}
