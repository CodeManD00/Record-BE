package com.example.record.auth.password;

import com.example.record.user.User;
import com.example.record.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordChangeService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    /**
     * 사용자 ID와 이전 비밀번호로 비밀번호 변경
     * JWT 인증 없이도 비밀번호 변경 가능 (이전 비밀번호 확인으로 인증)
     * 
     * @param req 비밀번호 변경 요청 (userId, oldPassword, newPassword 포함)
     */
    @Transactional
    public void changePassword(PasswordChangeRequest req) {
        // 사용자 조회
        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 이전 비밀번호 확인
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호로 변경
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }
}
