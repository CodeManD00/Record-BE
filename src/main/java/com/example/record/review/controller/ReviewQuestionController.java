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
     * @param user 현재 인증된 사용자 (JWT 토큰에서 자동으로 주입됨)
     * @param genre 장르 (예: "밴드", "연극/뮤지컬", "뮤지컬")
     * @return 질문 텍스트 목록
     * 
     * 응답 형식:
     * - 성공: { "success": true, "data": ["질문1", "질문2", "질문3"], "message": "질문 조회 성공" }
     * - 실패: { "success": false, "data": null, "message": "에러 메시지" }
     * 
     * 왜 @AuthenticationPrincipal을 사용하나요?
     * 1. 보안: JWT 토큰에서 사용자 정보를 안전하게 추출합니다.
     * 2. 편의성: 수동으로 토큰을 파싱할 필요가 없습니다.
     * 3. 일관성: 다른 컨트롤러들과 동일한 방식으로 사용자 정보를 가져옵니다.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getQuestions(
            @AuthenticationPrincipal User user,
            @RequestParam String genre) {
        try {
            // 인증된 사용자 확인
            if (user == null) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, null, "인증된 사용자 정보를 찾을 수 없습니다.")
                );
            }

            // 사용자 ID와 장르를 기반으로 질문 조회
            // ReviewQuestionService가 사용자의 티켓 개수에 따라 적절한 질문을 반환합니다.
            // - 티켓 3개 이하: DB의 기본 질문 템플릿에서 장르별 랜덤 질문 제공
            // - 티켓 3개 이상: 사용자 맞춤 질문 풀에서 랜덤 질문 제공 (없으면 기본 질문 사용)
            List<String> questions = reviewQuestionService.getQuestionsForUser(user.getId(), genre);

            // 성공 응답 반환
            return ResponseEntity.ok(
                new ApiResponse<>(true, questions, "질문 조회 성공")
            );
        } catch (Exception e) {
            // 예외 발생 시 에러 응답 반환
            // 왜 try-catch를 사용하나요?
            // 1. 예상치 못한 오류가 발생해도 서버가 중단되지 않음
            // 2. 사용자에게 적절한 오류 메시지 전달 가능
            // 3. 로그를 통해 문제 원인 파악 용이
            return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, null, "질문을 가져올 수 없습니다: " + e.getMessage())
            );
        }
    }
}

