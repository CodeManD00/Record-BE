package com.example.record.STTorText.review;



import com.example.record.auth.security.AuthUser;
import com.example.record.entity.Transcription;
import com.example.record.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/summarize")
    public ResponseEntity<?> summarize(
            @RequestBody ReviewRequest req,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        if (authUser == null) return ResponseEntity.status(401).build();
        User user = authUser.getUser();

        Transcription result = reviewService.summarize(req, user);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/organize")
    public ResponseEntity<?> organize(
            @RequestBody ReviewRequest req,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        if (authUser == null) return ResponseEntity.status(401).build();
        User user = authUser.getUser();

        Transcription result = reviewService.organize(req, user);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/finalize")
    public ResponseEntity<?> finalizeReview(
            @RequestBody FinalizeRequest req,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        if (authUser == null) return ResponseEntity.status(401).build();
        User user = authUser.getUser();

        Transcription result = reviewService.finalizeReview(req, user);
        return ResponseEntity.ok(result);
    }
}
