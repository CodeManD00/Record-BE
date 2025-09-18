package com.example.record.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일로 사용자 조회
    Optional<User> findByEmail(String email);

    // 해당 이메일이 이미 존재하는지 여부 확인
    boolean existsByEmail(String email);
}
