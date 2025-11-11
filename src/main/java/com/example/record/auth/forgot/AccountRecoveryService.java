package com.example.record.auth.forgot;

import com.example.record.auth.forgot.entity.PasswordResetToken;
import com.example.record.auth.forgot.entity.PasswordResetTokenRepository;
import com.example.record.user.User;
import com.example.record.user.UserRepository;


import com.example.record.auth.forgot.MailService;
import com.example.record.auth.forgot.ForgotIdRequest;
import com.example.record.auth.forgot.ForgotPasswordRequest;
import com.example.record.auth.forgot.ResetPasswordRequest;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;


@Service
@RequiredArgsConstructor
public class AccountRecoveryService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.base-url}")
    private String baseUrl;  // 경고만 뜨는 건 정상. 런타임에 주입됨.

    @Value("${app.mail.reset-token-ttl-minutes:30}")
    private int tokenTtlMinutes;

    private static final SecureRandom RANDOM = new SecureRandom();

    /** 1) 아이디 찾기: 이메일로 아이디 발송 */
    @Transactional(readOnly = true)
    public void sendLoginIdByEmail(ForgotIdRequest req) {
        userRepository.findByEmail(req.getEmail()).ifPresent(user -> {
            // 로그인용 ID = PK인 String id
            String loginId = user.getId();               // ⬅️ getLoginId() → getId()
            String subject = "[Re:cord] 가입 아이디 안내";
            String body = """
                    안녕하세요.
                    요청하신 가입 아이디를 안내드립니다.

                    아이디: %s

                    본 메일은 요청에 의해 발송되었습니다. 본인이 요청하지 않았다면 문의해 주세요.
                    """.formatted(loginId);
            mailService.send(user.getEmail(), subject, body); // ⬅️ getEmail() 필드명 확인
        });
    }

    /** 2) 비밀번호 재설정 링크 발송 */
    @Transactional
    public void sendResetPasswordLink(ForgotPasswordRequest req) {
        // ⬇️ findByLoginIdAndEmail → PK 기반 findByIdAndEmail
        User user = userRepository.findByIdAndEmail(req.getId(), req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("일치하는 계정을 찾을 수 없습니다."));

        String rawToken = generateSecureToken();
        String tokenHash = sha256(rawToken);
        Instant expiresAt = Instant.now().plus(tokenTtlMinutes, ChronoUnit.MINUTES);

        PasswordResetToken prt = PasswordResetToken.builder()
                .userId(user.getId())        // ⬅️ String으로 저장
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .used(false)
                .createdAt(Instant.now())
                .build();
        tokenRepository.save(prt);

        String link = baseUrl + "/reset-password?token=" + rawToken;
        String subject = "[Re:cord] 비밀번호 재설정 안내";
        String body = """
                안녕하세요.
                아래 링크를 통해 비밀번호를 재설정해 주세요. (유효기간: %d분)

                %s

                본인이 요청하지 않았다면 링크를 사용하지 말고 문의해 주세요.
                """.formatted(tokenTtlMinutes, link);
        mailService.send(user.getEmail(), subject, body);
    }

    /** 3) 실제 비밀번호 변경 */
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        String tokenHash = sha256(req.getToken());
        PasswordResetToken token = tokenRepository
                .findByTokenHashAndUsedFalseAndExpiresAtAfter(tokenHash, Instant.now())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않거나 만료된 토큰입니다."));

        // ⬇️ CrudRepository.findById(String)과 타입 일치
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        token.setUsed(true);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(md.digest(input.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not supported", e);
        }
    }
}
