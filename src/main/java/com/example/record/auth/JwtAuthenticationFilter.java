package com.example.record.auth;

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

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Authorization 헤더가 없거나 포맷이 잘못되면 다음 필터로 진행
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            if (!jwtUtil.validateToken(token)) {
                unauthorized(response, "Unauthorized: Invalid or expired token");
                return;
            }

            // subject = username (JwtUtil에서 subject를 username으로 발급)
            String username = jwtUtil.getUsernameFromToken(token);
            String roleFromToken = jwtUtil.getRoleFromToken(token);
            String effectiveRole = (roleFromToken != null && !roleFromToken.isBlank()) ? roleFromToken : "USER";

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Repository는 username 기반으로 조회 (findByEmail 사용 안 함)
                User user = userRepository.findByUsername(username).orElse(null);
                if (user != null) {
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + effectiveRole));

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, authorities);

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
