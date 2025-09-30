package com.example.record.user;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 아이디 (로그인용)
    @Column(nullable = false, unique = true)
    private String username;

    // 이메일 (중복 불가)
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    // 닉네임 (중복 가능)
    private String nickname;
}
