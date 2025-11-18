package com.example.record.STTorText.text;

import com.example.record.STTorText.gpt.GptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TextService {

    private final GptService gptService;

    /** 1) 5줄 요약 */
    public String summarize(String text) {
        String prompt = """
                아래 내용을 5줄 이내로 핵심만 요약해줘.
                - 내용 누락 없이 핵심만 압축
                - 불필요한 수식어 제거
                - 리스트가 아닌 자연스러운 문단
                
                원문:
                """ + text;

        return gptService.ask(prompt);
    }

    /** 2) 원문을 자르지 않고 정리 */
    public String organize(String text) {
        String prompt = """
                아래 글을 원문 분량을 유지하면서 더 읽기 좋게 재배치·정리해줘.
                - 문장 순서는 자연스럽게 조정 가능
                - 내용 삭제 금지
                - 요약 금지
                - 읽기 쉽게 단락만 정돈
                
                원문:
                """ + text;

        return gptService.ask(prompt);
    }
}
