package com.example.record.STTorText.review;

import com.example.record.STTorText.dto.SummaryResponse;
import com.example.record.promptcontrol_w03.service.OpenAIChatService;
import com.example.record.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ReviewServiceForBoth {

    private final OpenAIChatService openAI;

    /** ===========================================================
     * ① 후기 정리 (말투 유지 / 길이 유지 / 자연스럽게 정돈)
     * =========================================================== */
    public SummaryResponse organize(ReviewRequest req, User user) {
        String input = req.text();
        if (!StringUtils.hasText(input)) {
            throw new IllegalArgumentException("review text is required");
        }

        String prompt = """
                아래 공연 후기를 '말투와 분위기를 최대한 유지'하면서
                자연스럽게 정돈된 한 문단으로 정리해줘.
                - 핵심만 정리하되 내용은 크게 축약하지 말 것
                - 말투, 감정선, 표현 분위기를 유지
                - 너무 딱딱하지 않고 사용자 후기 느낌을 살릴 것
                - 불필요한 반복/오타/비문만 자연스럽게 고치기
                후기:
                %s
                """.formatted(input);

        String organized = openAI.complete(
                "You rewrite Korean text naturally while keeping the user's tone.",
                prompt
        );

        return new SummaryResponse(organized);
    }

    /** ===========================================================
     * ② 영어 3~5줄 요약 (이미지 basePrompt 용)
     * =========================================================== */
    public SummaryResponse summarize(ReviewRequest req, User user) {
        String base = req.text();
        if (!StringUtils.hasText(base)) {
            throw new IllegalArgumentException("review text is required");
        }

        String prompt = """
            Summarize the following Korean performance review into **3 to 5 full sentences in natural English**.
            Requirements:
            - Focus on core scenes, atmosphere, emotions, and spatial/mood elements.
            - No bullet points or lists.
            - No meta comments about the summary.
            - Make it suitable as a base prompt for an image-generation model.
            - Do NOT mention text, captions, or logos.

            Review:
            %s
            """.formatted(base);

        String summary = openAI.complete(
                "You translate and summarize Korean text into natural English suitable for image prompt usage.",
                prompt
        );

        return new SummaryResponse(summary);
    }
}
