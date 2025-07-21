package com.example.record.STT;

import com.example.record.DB.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stt")
public class SttController {

    private final SttService sttService;
    private final SttGptService sttGptService;
    private final TranscriptionRepository transcriptionRepository;

    // [1] 음성 파일 업로드 + STT 결과 저장
    @PostMapping
    public ResponseEntity<String> transcribe(@RequestParam MultipartFile file,
                                             @AuthenticationPrincipal User user) throws Exception {
        String fileName = UUID.randomUUID() + ".wav";
        String tempPath = "/tmp/" + fileName;

        File tempFile = new File(tempPath);
        file.transferTo(tempFile);

        String result = sttService.transcribeLocalFile(tempPath);
        tempFile.delete();

        Transcription transcription = Transcription.builder()
                .fileName(fileName)
                .resultText(result)
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();

        transcriptionRepository.save(transcription);

        return ResponseEntity.ok(result);
    }

    // [2] 본인 STT 기록 리스트 조회
    @GetMapping("/list")
    public ResponseEntity<List<TranscriptionResponse>> listMyTranscriptions(
            @AuthenticationPrincipal User user) {
        List<Transcription> transcriptions = transcriptionRepository.findByUser(user);

        List<TranscriptionResponse> response = transcriptions.stream()
                .map(t -> new TranscriptionResponse(
                        t.getId(),
                        t.getFileName(),
                        t.getResultText(),
                        t.getCreatedAt(),
                        t.getSummary(),
                        t.getQuestion()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    // [3] GPT 요약 + 질문 생성 → DB 저장 + 반환
    @PostMapping("/gpt")
    public ResponseEntity<GptResponse> summarizeAndQuestion(@RequestParam Long id,
                                                            @AuthenticationPrincipal User user) {
        Transcription t = transcriptionRepository.findById(id)
                .filter(tr -> tr.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("해당 기록이 없거나 권한이 없습니다."));

        String summary = sttGptService.summarize(t.getResultText());
        String question = sttGptService.generateQuestion(t.getResultText());

        t.setSummary(summary);
        t.setQuestion(question);
        transcriptionRepository.save(t);

        return ResponseEntity.ok(new GptResponse(summary, question));
    }
}
