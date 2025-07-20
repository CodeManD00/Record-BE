package com.example.record.DB;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class SignupRequest {
    private String username;
    private String email;
    private String password;

}