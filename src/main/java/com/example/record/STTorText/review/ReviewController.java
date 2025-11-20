package com.example.record.STTorText.review;



import com.example.record.auth.security.AuthUser;
import com.example.record.STTorText.entity.Transcription;
import com.example.record.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewServiceForBoth reviewService;

    /** 후기 정리 (말투 유지, 내용 그대로, 길이 유지) */
    @PostMapping("/organize")
    public ResponseEntity<?> organize(
            @RequestBody ReviewRequest req,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        if (authUser == null) return ResponseEntity.status(401).build();
        User user = authUser.getUser();

        return ResponseEntity.ok(reviewService.organize(req, user));
    }

    /** 후기 5줄 요약 */
    @PostMapping("/summarize")
    public ResponseEntity<?> summarize(
            @RequestBody ReviewRequest req,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        if (authUser == null) return ResponseEntity.status(401).build();
        User user = authUser.getUser();

        return ResponseEntity.ok(reviewService.summarize(req, user));
    }
}
