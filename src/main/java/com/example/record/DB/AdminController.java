// AdminController: 관리자 전용 대시보드 페이지를 제공하는 간단한 컨트롤러입니다.

package com.example.record.DB;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    // GET /admin/dashboard 요청 시 관리자용 메시지 반환
    @GetMapping("/dashboard")
    public String adminDashboard() {
        return "관리자 전용 페이지입니다.";
    }
}
