// JwtUtil: JWT 토큰을 생성하고, 유효성 검증 및 토큰에서 이메일/역할 정보를 추출하는 유틸리티 클래스입니다.

package com.example.record.DB;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String SECRET_KEY = "12345678901234567890123456789012"; // 최소 32바이트 이상의 시크릿 키
    private static final long EXPIRATION_MS = 1000 * 60 * 60; // 토큰 만료 시간: 1시간

    // HMAC SHA 알고리즘을 위한 서명 키 생성
    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    // JWT 토큰 생성: 이메일과 역할 정보를 클레임으로 포함
    public String generateToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)                      // 이메일을 subject로 설정
                .claim("role", role)                   // 역할 정보를 클레임에 포함
                .setIssuedAt(new Date())               // 발급 시간
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS)) // 만료 시간
                .signWith(key, SignatureAlgorithm.HS256) // 서명 알고리즘 및 키 설정
                .compact();                            // 토큰 문자열로 반환
    }

    // JWT 토큰의 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false; // 유효하지 않거나 만료된 경우
        }
    }

    // 토큰에서 이메일(subject) 추출
    public String getEmailFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key).build()
                .parseClaimsJws(token)
                .getBody().getSubject();
    }

    // 토큰에서 역할(role) 정보 추출
    public String getRoleFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key).build()
                .parseClaimsJws(token)
                .getBody().get("role", String.class);
    }
}
