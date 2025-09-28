package com.example.record.STT;


import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class QuestionService {

    // 카테고리별 미리 정의된 질문들
    private static final Map<String, List<String>> PRESET_QUESTIONS = Map.of(
            "뮤지컬", List.of(
                    "오늘 공연에서 가장 인상 깊었던 장면은 무엇이었나요?",
                    "주연 배우의 연기나 가창력은 어떠셨나요?",
                    "무대 연출이나 세트 디자인에서 특별했던 점이 있었나요?",
                    "이 작품의 메시지가 본인에게 어떤 의미로 다가왔나요?",
                    "음악과 가사 중 기억에 남는 넘버가 있으신가요?",
                    "앙상블이나 군무 장면은 어떠셨나요?",
                    "재관람 의사가 있으신가요? 그 이유는?",
                    "다른 사람에게 추천한다면 어떤 점을 강조하시겠어요?"
            ),
            "연극", List.of(
                    "배우들의 감정 전달이 어떠셨나요?",
                    "대사 전달력과 발성은 만족스러우셨나요?",
                    "작품의 주제나 메시지가 잘 전달되었나요?",
                    "무대 전환이나 소품 활용은 어떠셨나요?",
                    "관객과의 소통이나 상호작용이 있었나요?",
                    "연출가의 해석이 독특했던 부분이 있나요?",
                    "극의 템포나 긴장감 유지는 어떠셨나요?",
                    "어떤 관객에게 이 작품을 추천하시겠어요?"
            ),
            "콘서트", List.of(
                    "세트리스트 구성은 만족스러우셨나요?",
                    "아티스트의 라이브 실력은 어떠셨나요?",
                    "관객과의 소통이나 멘트는 어떠셨나요?",
                    "음향이나 조명 연출은 만족스러우셨나요?",
                    "앙코르나 특별 무대가 있었나요?",
                    "가장 감동적이었던 곡은 무엇이었나요?",
                    "공연장 분위기는 어떠셨나요?",
                    "티켓 값 대비 만족도는 어느 정도인가요?"
            ),
            "전시", List.of(
                    "전시 동선과 구성은 어떠셨나요?",
                    "가장 인상 깊었던 작품은 무엇이었나요?",
                    "작품 설명이나 도슨트는 도움이 되었나요?",
                    "전시 공간의 분위기는 어떠셨나요?",
                    "작가의 의도가 잘 전달되었다고 생각하시나요?",
                    "관람 시간은 적절했나요?",
                    "사진 촬영이 가능했나요? 기념품샵은 어떠셨나요?",
                    "비슷한 전시와 비교했을 때 특별한 점이 있었나요?"
            ),
            "기본", List.of(
                    "오늘 공연/전시의 전반적인 만족도는 어느 정도인가요?",
                    "가장 기억에 남는 순간은 무엇이었나요?",
                    "아쉬웠던 점이 있다면 무엇인가요?",
                    "기대했던 것과 비교해서 어떠셨나요?",
                    "함께 관람한 사람들의 반응은 어떠했나요?",
                    "이 경험이 본인에게 어떤 영향을 미쳤나요?",
                    "재방문 의사가 있으신가요?",
                    "다른 사람에게 추천하실 의향이 있으신가요?"
            )
    );

    /**
     * 카테고리별 미리 정의된 질문 반환
     */
    public List<String> getPresetQuestions(String category) {
        if (category == null || category.isEmpty()) {
            category = "기본";
        }
        return PRESET_QUESTIONS.getOrDefault(category, PRESET_QUESTIONS.get("기본"));
    }

    /**
     * 모든 카테고리 목록 반환
     */
    public List<String> getCategories() {
        return List.copyOf(PRESET_QUESTIONS.keySet());
    }
}