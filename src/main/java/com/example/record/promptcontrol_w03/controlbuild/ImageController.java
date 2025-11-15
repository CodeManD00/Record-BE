package com.example.record.promptcontrol_w03.controlbuild;
/*
역할: 이미지 생성 API 엔드포인트.

엔드포인트
POST /generate-image        : 실제 생성 (기본 흐름 또는 basePrompt가 오면 재생성 흐름)  ← multipart/form-data (JSON + file)
POST /generate-image/test   : 더미(placeholder) 이미지 응답 (API 키/연동 이슈 시 테스트용)  ← application/json

로직
- (재생성) request.basePrompt 가 있으면: basePrompt + imageRequest 를 합쳐 즉시 이미지 생성
- (기본)   PromptService 로 최종 프롬프트 생성 → Gpt1PicService.generateSingleImageUrl() 호출
- ImageResponse(prompt, imageUrl) 반환

특징: 프롬프트/길이 디버그 출력, 예외 시 400/500 처리
*/
import com.example.record.promptcontrol_w03.dto.PromptRequest;
import com.example.record.promptcontrol_w03.dto.PromptResponse;
import com.example.record.promptcontrol_w03.dto.ImageResponse;
import com.example.record.promptcontrol_w03.service.Gpt1PicService;
import com.example.record.promptcontrol_w03.service.PromptService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/generate-image")
public class ImageController {

    private static final String NO_TEXT_RULE =
            " No captions, no letters, no words, no logos, no watermarks.";

    private final PromptService promptService;
    private final Gpt1PicService gpt1PicService;

    public ImageController(PromptService promptService, Gpt1PicService gpt1PicService) {
        this.promptService = promptService;
        this.gpt1PicService = gpt1PicService;
    }

    /** ✅ JSON만 받는 실제 생성 엔드포인트 (파일 없이) */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ImageResponse> generateImage(@RequestBody PromptRequest request) {
        return generateImageInternal(request, null);
    }

    /** ✅ JSON + 파일을 함께 받는 실제 생성 엔드포인트 */
    @PostMapping(value = "/with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageResponse> generateImageWithFile(
            @RequestPart("request") PromptRequest request,                           // JSON 파트
            @RequestPart(value = "file", required = false) MultipartFile file       // 파일 파트(선택)
    ) {
        return generateImageInternal(request, file);
    }

    /** 내부 이미지 생성 로직 (공통 처리) */
    private ResponseEntity<ImageResponse> generateImageInternal(PromptRequest request, MultipartFile file) {
        try {
            // (선택) 장르 검증: 뮤지컬/밴드 아니면 400 (단, basePrompt 재생성 모드는 스킵)
            if ((request.getBasePrompt() == null || request.getBasePrompt().isBlank())
                    && request.getGenre() != null
                    && !(request.getGenre().equals("뮤지컬") || request.getGenre().equals("밴드"))) {
                return ResponseEntity.badRequest().body(ImageResponse.error("Unsupported genre"));
            }

            final String finalPrompt;

            // 0) 재생성 모드: basePrompt + imageRequest (분석 재호출 없이 즉시)
            if (request.getBasePrompt() != null && !request.getBasePrompt().isBlank()) {
                finalPrompt = buildMergedPrompt(request.getBasePrompt(), request.getImageRequest());
            } else {
                // 1) 기본 모드: 프롬프트 새로 생성
                PromptResponse promptResponse = promptService.generatePrompt(request);
                finalPrompt = ensureNoTextRule(promptResponse.getPrompt());
            }

            // (참고) file이 필요한 추가 로직이 있으면 여기에서 처리하면 됨.
            // if (file != null && !file.isEmpty()) { ... }

            // 디버깅: 최종 프롬프트 로그 출력
            System.out.println("🔍 [DEBUG] 최종 프롬프트 (이미지 생성 전):");
            System.out.println(finalPrompt);
            System.out.println("🔍 [DEBUG] 프롬프트 길이: " + finalPrompt.length() + " 문자");

            // 2) 단일 이미지 생성 (항상 1장, 4:5)
            String imageUrl = gpt1PicService.generateSingleImageUrl(finalPrompt);

            // 3) 응답
            ImageResponse response = new ImageResponse();
            response.setPrompt(finalPrompt);
            response.setImageUrl(imageUrl);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ImageResponse.error(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ImageResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    //  임시 테스트 엔드포인트 (JSON 단독) - 삭제


    /** basePrompt 뒤에 extra(imageRequest)를 자연스럽게 덧붙이고, NO_TEXT_RULE을 보증한다. */
    private String buildMergedPrompt(String basePrompt, String extra) {
        String merged = basePrompt == null ? "" : basePrompt.trim();
        if (extra != null && !extra.isBlank()) {
            merged = merged + " " + extra.trim();
        }
        return ensureNoTextRule(merged);
    }

    /** 이미 NO_TEXT_RULE이 있으면 중복 추가하지 않음 */
    private String ensureNoTextRule(String prompt) {
        String p = prompt == null ? "" : prompt.trim();
        if (!p.contains("No captions, no letters")) {
            p = p + NO_TEXT_RULE;
        }
        return p;
    }
}
