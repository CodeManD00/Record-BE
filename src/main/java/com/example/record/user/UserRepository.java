package com.example.record.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {  // Long -> String으로 변경
    boolean existsByEmail(String email);
    // findByEmail 메서드는 이미 findById로 대체됨 (id가 String이므로)
}