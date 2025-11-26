package com.example.record.review.controller;

import com.example.record.common.ApiResponse;
import com.example.record.review.service.ReviewQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.example.record.user.User;

import java.util.List;

/**
 * 후기 작성 지원 질문 컨트롤러
 * 
 * 역할: 사용자에게 표시할 질문들을 제공하는 API
 * 
 * 왜 이 컨트롤러가 필요한가요?
 * 1. 질문 제공: 사용자가 후기를 작성할 때 도움이 되는 질문들을 제공합니다.
 * 2. 개인화: 사용자의 티켓 개수와 장르에 따라 적절한 질문을 제공합니다.
 * 3. API 분리: 리뷰 관리와 질문 제공을 분리하여 코드의 책임을 명확히 합니다.
 */
@RestController
@RequestMapping("/review-questions")
@RequiredArgsConstructor
public class ReviewQuestionController {

    private final ReviewQuestionService reviewQuestionService;

    /**
     * 사용자에게 표시할 질문들을 가져옵니다.
     *
     * @param user  현재 인증된 사용자 (JWT 토큰에서 자동으로 주입됨)
     * @param xUserId X-User-Id 헤더에서 추출한 사용자 ID (JWT 토큰이 없을 때 사용)
     * @param genre 장르 (예: "밴드", "연극/뮤지컬", "뮤지컬")
     * @return 질문 텍스트 목록
     * <p>
     * 응답 형식:
     * - 성공: { "success": true, "data": ["질문1", "질문2", "질문3"], "message": "질문 조회 성공" }
     * - 실패: { "success": false, "data": null, "message": "에러 메시지" }
     * <p>
     * 사용자 ID 추출 우선순위:
     * 1. JWT 토큰에서 추출한 사용자 ID (가장 우선)
     * 2. X-User-Id 헤더에서 추출한 사용자 ID
     * 3. 둘 다 없으면 null (기본 질문 제공)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getQuestions(
            @AuthenticationPrincipal User user,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestParam("genre") String genre) {
        try {
            // ★ 사용자 ID 추출: JWT 우선, 없으면 X-User-Id 헤더 사용
            String userId = null;
            if (user != null) {
                userId = user.getId();
            } else if (xUserId != null && !xUserId.trim().isEmpty()) {
                userId = xUserId.trim();
            }

            // userId가 null이면 ReviewQuestionService에서 기본 질문 제공
            List<String> questions = reviewQuestionService.getQuestionsForUser(userId, genre);

            return ResponseEntity.ok(
                    new ApiResponse<>(true, questions, "질문 조회 성공")
            );

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, null, "질문을 가져올 수 없습니다: " + e.getMessage())
            );
        }
    }
}

