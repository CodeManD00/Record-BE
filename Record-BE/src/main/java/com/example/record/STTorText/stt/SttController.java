package com.example.record.STTorText.stt;

import com.example.record.STTorText.entity.Transcription;
import com.example.record.STTorText.entity.TranscriptionRepository;
import com.example.record.user.User;
import com.example.record.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stt")
public class SttController {

    private final WhisperService whisperService;
    private final SttService sttService;
    private final TranscriptionRepository repo;
    private final UserRepository userRepository;

    /**
     * STT 변환 및 저장 (JWT 토큰 불필요)
     */
    @PostMapping("/transcribe-and-save")
    public ResponseEntity<?> transcribe(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId
    ) {
        // 사용자 ID 유효성 검사
        if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("사용자 ID를 입력해주세요.");
        }

        // 사용자 존재 여부 확인
        User user = userRepository.findById(userId.trim())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        try {
            System.out.println("=== STT 요청 정보 ===");
            System.out.println("원본 파일명: " + file.getOriginalFilename());
            System.out.println("Content-Type: " + file.getContentType());
            System.out.println("파일 크기: " + file.getSize() + " bytes");
            
            byte[] bytes = file.getBytes();
            // 모든 파일을 m4a 형식으로 변환 (Whisper API 호환성 보장)
            bytes = sttService.maybeReencodeToM4a(bytes, file.getOriginalFilename());
            
            // 변환 후 파일명을 .m4a로 변경
            String filename = file.getOriginalFilename();
            if (filename != null && filename.contains(".")) {
                filename = filename.substring(0, filename.lastIndexOf('.')) + ".m4a";
            } else {
                filename = "audio.m4a";
            }

            String transcript = whisperService.transcribe(bytes, filename, "ko");

            System.out.println("=== 변환된 텍스트 ===");
            System.out.println("텍스트 길이: " + (transcript != null ? transcript.length() : 0) + " 문자");
            System.out.println("텍스트 내용 (처음 100자): " + (transcript != null && transcript.length() > 100 ? transcript.substring(0, 100) + "..." : transcript));

            Transcription t = Transcription.builder()
                    .user(user)
                    .fileName(file.getOriginalFilename())
                    .resultText(transcript)
                    .summary(null)
                    .summaryType(null)
                    .createdAt(LocalDateTime.now())
                    .build();

            Transcription saved = repo.save(t);
            System.out.println("=== DB 저장 완료 ===");
            System.out.println("저장된 ID: " + saved.getId());
            System.out.println("저장된 resultText 길이: " + (saved.getResultText() != null ? saved.getResultText().length() : 0) + " 문자");

            return ResponseEntity.ok(t);

        } catch (Exception e) {
            return ResponseEntity.status(422).body("STT 변환 실패: " + e.getMessage());
        }
    }
}
