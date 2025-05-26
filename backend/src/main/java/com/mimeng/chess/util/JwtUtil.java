package com.mimeng.chess.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
  private static final long EXPIRATION = 1000 * 60 * 60 * 24; // 24小时

  private static String staticSecretKey;

  @Value("${jwt.secret.key}")
  private String secretKey;

  @PostConstruct
  private void init() {
    staticSecretKey = secretKey;
  }

  public static String generateToken(String userId, String email) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    claims.put("email", email);
    System.out.println(staticSecretKey);
    return Jwts.builder()
        .setClaims(claims)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
        .signWith(SignatureAlgorithm.HS256, staticSecretKey)
        .compact();
  }

  public static Claims parseToken(String token) {
    return Jwts.parser()
        .setSigningKey(staticSecretKey)
        .parseClaimsJws(token)
        .getBody();
  }
}
