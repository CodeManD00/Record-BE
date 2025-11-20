package com.example.record.promptcontrol_w03.controlbuild;

import com.example.record.promptcontrol_w03.dto.ImageResponse;
import com.example.record.promptcontrol_w03.dto.PromptRequest;
import com.example.record.promptcontrol_w03.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/generate-image")
public class ImageController {

    private static final String NO_TEXT_RULE =
            "No captions, no letters, no words, no logos, no watermarks.";

    private final ImageService imageService;

    /** ★ JSON 기반 이미지 생성 */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ImageResponse> generateJson(@RequestBody PromptRequest request) {
        return ResponseEntity.ok(
                generateInternal(request.getBasePrompt(), request.getImageRequest())
        );
    }

    /** ★ JSON + 파일 기반 이미지 생성 */
    @PostMapping(value = "/with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageResponse> generateWithFile(
            @RequestPart("request") PromptRequest request,
            @RequestPart(value = "file", required = false) MultipartFile ignoredFile
    ) {
        return ResponseEntity.ok(
                generateInternal(request.getBasePrompt(), request.getImageRequest())
        );
    }

    /** ★ 최종 이미지 생성 공통 처리 */
    private ImageResponse generateInternal(String basePrompt, String imageRequest) {

        if (basePrompt == null || basePrompt.isBlank()) {
            return ImageResponse.error("basePrompt is required (English summary text).");
        }

        String finalPrompt = buildPrompt(basePrompt, imageRequest);

        String imageUrl = imageService.generateImage(finalPrompt);

        ImageResponse res = new ImageResponse();
        res.setPrompt(finalPrompt);
        res.setImageUrl(imageUrl);

        return res;
    }

    /** ★ 프롬프트 최종 조립 */
    private String buildPrompt(String base, String extra) {
        StringBuilder sb = new StringBuilder();
        sb.append(base.trim());

        if (extra != null && !extra.isBlank()) {
            sb.append(" Additional request: ").append(extra.trim()).append(".");
        }

        String result = sb.toString().trim();

        if (!result.contains("No captions")) {
            result += " " + NO_TEXT_RULE;
        }

        return result;
    }
}
