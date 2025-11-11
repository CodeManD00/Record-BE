
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
            User devUser = userRepository.findById("dev").orElseGet(() -> {
                User u = User.builder()
                        .id("dev")
                        .email("dev@local")
                        .password(passwordEncoder.encode("devpass"))
                        .nickname("DEV")
                        .role("USER")
                        .build();
                return userRepository.save(u);
            });

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
