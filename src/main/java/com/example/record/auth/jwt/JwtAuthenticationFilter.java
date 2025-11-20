package com.example.record.auth.jwt;

import com.example.record.auth.security.AuthUser;
import com.example.record.user.User;
import com.example.record.user.UserRepository;
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
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    /** ===== JWT 검사 제외 경로(SAFE LIST) ===== */
    private static final String[] EXCLUDE_PATHS = {
            "/auth",
            "/reviews",
            "/stt",
            "/ocr",
            "/generate-image",
            "/STTorText",
            "/review-questions"
    };

    private boolean isExcluded(String path) {
        for (String prefix : EXCLUDE_PATHS) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();

        /** ===== 1) 허용 경로면 JWT 검사하지 않고 바로 통과 ===== */
        if (isExcluded(path)) {
            chain.doFilter(request, response);
            return;
        }

        /** ===== 2) 여기부터 JWT 검사 ===== */
        final String authHeader = request.getHeader("Authorization");

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            if (jwtUtil.validateToken(token)) {
                String id = jwtUtil.getIdFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);

                User user = userRepository.findById(id).orElse(null);

                if (user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(new AuthUser(user), null, authorities);

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (JwtException | IllegalArgumentException e) {
            unauthorized(response, "Unauthorized: Invalid or expired token");
            return;
        }

        chain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\": \"" + msg + "\"}");
    }
}
