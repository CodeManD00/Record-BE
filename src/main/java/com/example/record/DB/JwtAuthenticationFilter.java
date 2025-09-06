package com.example.record.DB;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final AntPathMatcher matcher = new AntPathMatcher();

    // 인증 없이 통과시킬 경로 (옵션 A: /ocr 단일 엔드포인트)
    private static final String[] WHITELIST = new String[]{
            "/auth/**",

            // ✅ OCR: /ocr 변형 전부 + context-path 포함 케이스까지
            "/ocr", "/ocr/", "/ocr/**",
            "/**/ocr", "/**/ocr/", "/**/ocr/**",

            // 🔓 STT 테스트 때만 잠깐 추가(운영 기본은 잠금)
            // "/stt",
            // "/stt/gpt",
            // "/stt/list",

            "/api/image/**"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String uri = request.getRequestURI();
        final boolean whitelisted = Arrays.stream(WHITELIST).anyMatch(p -> matcher.match(p, uri));
        // 간단 디버그 로그 (원하면 log.debug로 교체)
        System.out.println("[JwtAuthFilter] " + request.getMethod() + " " + uri + " -> whitelisted=" + whitelisted);

        if (whitelisted) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtUtil.validateToken(token)) {
                    String email = jwtUtil.getEmailFromToken(token);
                    String role = jwtUtil.getRoleFromToken(token);
                    User user = userRepository.findByEmail(email).orElse(null);

                    if (user != null) {
                        List<SimpleGrantedAuthority> authorities =
                                List.of(new SimpleGrantedAuthority("ROLE_" + role));
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(user, null, authorities);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (JwtException | IllegalArgumentException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\": \"Unauthorized: Invalid or expired token\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
