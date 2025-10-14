package com.example.record.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String SECRET_KEY = "12345678901234567890123456789012";
    private static final long EXPIRATION_MS = 1000 * 60 * 60;

    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    /**
     * JWT 토큰을 생성합니다.
     * 
     * 변경 사항:
     * - email → id로 변경
     * - 이유: username 용어를 id로 통일하기 때문
     * 
     * @param id 사용자 ID
     * @param role 사용자 역할
     * @return 생성된 JWT 토큰
     */
    public String generateToken(String id, String role) {
        return Jwts.builder()
                .setSubject(id)  // email → id로 변경
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 단일 파라미터로 토큰을 생성합니다.
     * 
     * 기존 코드와의 호환성을 위해 유지합니다.
     * 
     * @param id 사용자 ID
     * @return 생성된 JWT 토큰 (기본 역할: USER)
     */
    public String generateToken(String id) {
        return generateToken(id, "USER");
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * 토큰에서 사용자 ID를 추출합니다.
     * 
     * 변경 사항:
     * - getEmailFromToken → getIdFromToken으로 변경
     * - 이유: username 용어를 id로 통일하기 때문
     * 
     * @param token JWT 토큰
     * @return 사용자 ID
     */
    public String getIdFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public String getRoleFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().get("role", String.class);
    }
}
