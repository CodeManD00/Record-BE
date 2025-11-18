package com.example.record.STTorText.text;


import com.example.record.STTorText.gpt.GptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/text")
public class TextController {

    private final GptService gpt;

    @PostMapping("/summarize")
    public ResponseEntity<?> summarize(@RequestBody TextRequest req) {
        String prompt = """
                아래 글을 5줄 이내로 요약해줘:

                """ + req.text();
        return ResponseEntity.ok(gpt.ask(prompt));
    }

    @PostMapping("/organize")
    public ResponseEntity<?> organize(@RequestBody TextRequest req) {
        String prompt = """
                아래 글을 원문 길이 그대로 읽기 좋게 정리해줘:

                """ + req.text();
        return ResponseEntity.ok(gpt.ask(prompt));
    }
}
