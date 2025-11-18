package com.example.record.STTorText.review;


import com.example.record.entity.Transcription;
import com.example.record.entity.TranscriptionRepository;
import com.example.record.gpt.GptService;
import com.example.record.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final GptService gpt;
    private final TranscriptionRepository repo;

    /** STT 기반 또는 텍스트 기반으로 자동 분기 */
    private String resolveBaseText(ReviewRequest req, User user) {
        if (req.transcriptionId() != null) {
            Transcription t = repo.findById(req.transcriptionId())
                    .filter(x -> x.getUser().equals(user))
                    .orElseThrow(() -> new RuntimeException("Transcription not found"));

            return StringUtils.hasText(req.text()) ? req.text() : t.getResultText();
        }
        return req.text();
    }

    /** 5줄 요약 */
    public Transcription summarize(ReviewRequest req, User user) {
        String base = resolveBaseText(req, user);

        String prompt = """
                아래 내용을 5줄 이내로 자연스럽게 요약해줘.
                내용 누락 없이 핵심만 압축.

                원문:
                """ + base;

        String summary = gpt.ask(prompt);

        if (req.transcriptionId() == null) return null;

        Transcription t = repo.findById(req.transcriptionId()).get();
        t.setSummary(summary);
        t.setSummaryType(ReviewType.SUMMARY);
        return repo.save(t);
    }

    /** 원문 정리 */
    public Transcription organize(ReviewRequest req, User user) {
        String base = resolveBaseText(req, user);

        String prompt = """
                아래 글을 원문 길이를 유지한 채 더 읽기 좋게 정리해줘.
                요약 금지, 삭제 금지.

                원문:
                """ + base;

        String organized = gpt.ask(prompt);

        if (req.transcriptionId() == null) return null;

        Transcription t = repo.findById(req.transcriptionId()).get();
        t.setSummary(organized);
        t.setSummaryType(ReviewType.ORGANIZED);
        return repo.save(t);
    }

    /** 최종 후기 */
    public Transcription finalizeReview(FinalizeRequest req, User user) {
        Transcription t = repo.findById(req.transcriptionId())
                .filter(x -> x.getUser().equals(user))
                .orElseThrow(() -> new RuntimeException("Transcription not found"));

        String base = StringUtils.hasText(t.getSummary()) ? t.getSummary() : t.getResultText();
        if (StringUtils.hasText(req.extraNotes()))
            base += "\n\n추가 메모: " + req.extraNotes();

        String prompt = """
                아래 내용을 블로그용 후기 글로 다듬어줘.
                - 감정 유지
                - 문장 구조 매끄럽게

                원문:
                """ + base;

        String finalReview = gpt.ask(prompt);

        t.setSummary(finalReview);
        t.setSummaryType(ReviewType.FINAL);
        return repo.save(t);
    }
}
