package com.example.record.STTorText.stt;

import com.example.record.STTorText.entity.Transcription;
import com.example.record.STTorText.entity.TranscriptionRepository;
import com.example.record.auth.security.AuthUser;
import com.example.record.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping("/transcribe-and-save")
    public ResponseEntity<?> transcribe(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        if (authUser == null) return ResponseEntity.status(401).body("Unauthorized");
        User user = authUser.getUser();

        try {
            byte[] bytes = file.getBytes();
            bytes = sttService.maybeReencodeToM4a(bytes, file.getOriginalFilename());

            String transcript = whisperService.transcribe(bytes, file.getOriginalFilename(), "ko");

            Transcription t = Transcription.builder()
                    .user(user)
                    .fileName(file.getOriginalFilename())
                    .resultText(transcript)
                    .summary(null)
                    .summaryType(null)
                    .createdAt(LocalDateTime.now())
                    .build();

            repo.save(t);

            return ResponseEntity.ok(t);

        } catch (Exception e) {
            return ResponseEntity.status(422).body("STT 변환 실패: " + e.getMessage());
        }
    }
}
