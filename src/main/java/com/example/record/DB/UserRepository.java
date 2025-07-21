// UserRepository: User 엔티티에 대한 CRUD 및 사용자 이메일 기반 조회 기능을 제공하는 JPA 리포지토리 인터페이스입니다.

package com.example.record.DB;

import com.example.record.DB.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일로 사용자 조회
    Optional<User> findByEmail(String email);

    // 해당 이메일이 이미 존재하는지 여부 확인
    boolean existsByEmail(String email);
}
