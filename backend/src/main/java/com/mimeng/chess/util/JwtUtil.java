package com.mimeng.chess.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
  private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
  private static final long EXPIRATION = 1000 * 60 * 60 * 24; // 24小时
  private SecretKey secretKey;

  @Value("${jwt.secret.key}")
  private String secretKeyString;

  @PostConstruct
  private void init() {
    try {
      // 使用HMAC-SHA256算法创建密钥
      this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes());
      logger.info("JWT secret key initialized successfully");
    } catch (Exception e) {
      logger.error("Failed to initialize JWT secret key: {}", e.getMessage(), e);
      throw new RuntimeException("JWT密钥初始化失败", e);
    }
  }

  public String generateToken(String userId, String email) {
    if (secretKey == null) {
      throw new IllegalStateException("JWT secret key not initialized");
    }
    try {
      Map<String, Object> claims = new HashMap<>();
      claims.put("userId", userId);
      claims.put("email", email);

      String token = Jwts.builder()
          .setClaims(claims)
          .setIssuedAt(new Date())
          .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
          .signWith(secretKey, SignatureAlgorithm.HS256)
          .compact();

      logger.debug("Generated JWT token for userId: {}, email: {}", userId, email);
      return token;
    } catch (Exception e) {
      logger.error("Failed to generate JWT token for userId: {}, email: {}", userId, email, e);
      throw new RuntimeException("JWT token生成失败", e);
    }
  }

  public Claims parseToken(String token) {
    if (secretKey == null) {
      throw new IllegalStateException("JWT secret key not initialized");
    }
    try {
      logger.debug("Parsing JWT token: {}...", token.substring(0, Math.min(20, token.length())));

      Claims claims = Jwts.parserBuilder()
          .setSigningKey(secretKey)
          .build()
          .parseClaimsJws(token)
          .getBody();

      logger.debug("JWT token parsed successfully, userId: {}, email: {}",
          claims.get("userId"), claims.get("email"));
      return claims;
    } catch (Exception e) {
      logger.error("Failed to parse JWT token: {}..., error: {}",
          token.substring(0, Math.min(20, token.length())), e.getMessage(), e);
      throw e;
    }
  }
}
