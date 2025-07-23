// 음성 파일을 업로드 받아 텍스트로 변환하고, 변환된 텍스트를 DB에 저장하며, 요약을 생성하는 컨트롤러입니다.
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

    private final SttService sttService; // 음성 파일을 텍스트로 변환하는 서비스
    private final SttGptService sttGptService; // 텍스트 요약을 생성하는 서비스
    private final TranscriptionRepository transcriptionRepository; // 데이터베이스와의 상호작용을 위한 리포지토리

    // [1] 음성 파일 업로드 → 텍스트로 변환 → DB 저장 (요약 X)
    @PostMapping
    public ResponseEntity<String> transcribe(@RequestParam MultipartFile file,
                                             @AuthenticationPrincipal User user) throws Exception {
        // UUID를 사용하여 고유한 파일 이름 생성
        String fileName = UUID.randomUUID() + ".wav";
        File tempFile = new File("/tmp/" + fileName); // 임시 파일 경로 설정

        // 업로드된 파일을 임시 파일로 저장
        file.transferTo(tempFile);

        // STT 서비스를 통해 음성 파일을 텍스트로 변환
        String result = sttService.transcribeLocalFile(tempFile.getAbsolutePath());

        // 임시 파일 삭제
        tempFile.delete();

        // 변환된 텍스트와 관련 정보를 DB에 저장
        Transcription t = Transcription.builder()
                .fileName(fileName)
                .resultText(result)
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();
        transcriptionRepository.save(t);

        // 변환된 텍스트 반환
        return ResponseEntity.ok(result);
    }

    // [2] 요약 생성 (질문 제거됨)
    @PostMapping("/gpt")
    public ResponseEntity<GptResponse> summarize(@RequestParam Long id,
                                                 @AuthenticationPrincipal User user) {
        // 주어진 ID로 기록을 조회하고, 해당 사용자의 기록인지 확인
        Transcription t = transcriptionRepository.findById(id)
                .filter(tr -> tr.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("해당 기록이 없거나 권한이 없습니다."));

        // STT GPT 서비스를 통해 텍스트 요약 생성
        String summary = sttGptService.summarize(t.getResultText());

        // 요약을 기록에 저장
        t.setSummary(summary);
        transcriptionRepository.save(t);

        // 생성된 요약 반환
        return ResponseEntity.ok(new GptResponse(summary));
    }

    // [3] 기록 조회
    @GetMapping("/list")
    public ResponseEntity<List<TranscriptionResponse>> list(@AuthenticationPrincipal User user) {
        // 사용자의 모든 기록을 조회
        List<Transcription> transcriptions = transcriptionRepository.findByUser(user);

        // 조회된 기록을 응답 형식으로 변환
        List<TranscriptionResponse> response = transcriptions.stream()
                .map(t -> new TranscriptionResponse(
                        t.getId(),
                        t.getFileName(),
                        t.getResultText(),
                        t.getCreatedAt(),
                        t.getSummary()
                )).toList();

        // 변환된 기록 목록 반환
        return ResponseEntity.ok(response);
    }
}
