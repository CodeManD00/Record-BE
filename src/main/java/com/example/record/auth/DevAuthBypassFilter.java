package com.example.record.auth;

import com.example.record.user.User;
import com.example.record.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Profile("dev")
@Component
@RequiredArgsConstructor
public class DevAuthBypassFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            // 1) dev 유저를 DB에서 찾고, 없으면 생성
            // 
            // 변경 사항:
            // - findByEmail → findById로 변경
            // - 이유: username을 id로 통일하면서 findByEmail 메서드를 제거했기 때문
            // - dev@local 이메일을 가진 사용자를 찾기 위해 이메일로 조회하는 대신
            //   고정된 dev 사용자 ID를 사용하도록 변경
            User devUser = userRepository.findById("dev").orElseGet(() -> {
                User u = User.builder()
                        .id("dev")  // 필수 필드인 id 추가
                        .email("dev@local")
                        .password(passwordEncoder.encode("devpass"))
                        .nickname("DEV")
                        .build();
                return userRepository.save(u);
            });

            // role은 USER로 고정 (User 엔티티에 role 없음)
            var auth = new UsernamePasswordAuthenticationToken(
                    devUser,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(req, res);
    }
}
