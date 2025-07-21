// SttController: 음성 파일을 업로드하여 텍스트로 변환(STT)하고, 요약 및 질문 생성을 통해 결과를 저장하고 조회하는 컨트롤러입니다.

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

    private final SttService sttService;                         // STT 변환 서비스
    private final SttGptService sttGptService;                   // GPT 요약/질문 생성 서비스
    private final TranscriptionRepository transcriptionRepository; // 변환 결과 저장소

    // [1] 음성 파일 업로드 → STT 실행 → 변환 결과를 DB에 저장
    @PostMapping
    public ResponseEntity<String> transcribe(@RequestParam MultipartFile file,
                                             @AuthenticationPrincipal User user) throws Exception {
        // 임시 파일로 저장
        String fileName = UUID.randomUUID() + ".wav";
        String tempPath = "/tmp/" + fileName;
        File tempFile = new File(tempPath);
        file.transferTo(tempFile);

        // 로컬 파일을 텍스트로 변환
        String result = sttService.transcribeLocalFile(tempPath);
        tempFile.delete(); // 변환 후 임시 파일 삭제

        // DB에 변환 결과 저장
        Transcription transcription = Transcription.builder()
                .fileName(fileName)
                .resultText(result)
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();
        transcriptionRepository.save(transcription);

        return ResponseEntity.ok(result);
    }

    // [2] 로그인한 사용자의 STT 기록 리스트 조회
    @GetMapping("/list")
    public ResponseEntity<List<TranscriptionResponse>> listMyTranscriptions(
            @AuthenticationPrincipal User user) {
        List<Transcription> transcriptions = transcriptionRepository.findByUser(user);

        // Transcription → TranscriptionResponse로 변환
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

    // [3] 선택한 기록에 대해 GPT 요약 및 질문 생성 → DB에 저장 후 결과 반환
    @PostMapping("/gpt")
    public ResponseEntity<GptResponse> summarizeAndQuestion(@RequestParam Long id,
                                                            @AuthenticationPrincipal User user) {
        // 본인 기록인지 확인 및 조회
        Transcription t = transcriptionRepository.findById(id)
                .filter(tr -> tr.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("해당 기록이 없거나 권한이 없습니다."));

        // GPT를 통해 요약 및 질문 생성
        String summary = sttGptService.summarize(t.getResultText());
        String question = sttGptService.generateQuestion(t.getResultText());

        // 결과 저장
        t.setSummary(summary);
        t.setQuestion(question);
        transcriptionRepository.save(t);

        return ResponseEntity.ok(new GptResponse(summary, question));
    }
}
