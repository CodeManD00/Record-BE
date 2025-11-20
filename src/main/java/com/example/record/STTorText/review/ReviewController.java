package com.example.record.STTorText.review;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/review")   // reviews → review
public class ReviewController {

    private final ReviewServiceForBoth reviewService;

    /** 후기 정리 */
    @PostMapping("/organize")
    public ResponseEntity<?> organize(
            @RequestBody ReviewRequest req
    ) {
        return ResponseEntity.ok(reviewService.organize(req, null));
    }

    /** 후기 5줄 요약 */
    @PostMapping("/summarize")
    public ResponseEntity<?> summarize(
            @RequestBody ReviewRequest req
    ) {
        return ResponseEntity.ok(reviewService.summarize(req, null));
    }
}
