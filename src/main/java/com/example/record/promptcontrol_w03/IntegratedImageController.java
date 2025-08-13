package com.example.record.promptcontrol_w03;

import com.example.record.promptcontrol_w03.dto.ImageResponse;
import com.example.record.promptcontrol_w03.dto.PromptRequest;
import com.example.record.promptcontrol_w03.dto.PromptResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/generate-image")
public class IntegratedImageController {

    private final PromptService promptService;
    private final Dalle3Service dalle3Service;

    public IntegratedImageController(PromptService promptService, Dalle3Service dalle3Service) {
        this.promptService = promptService;
        this.dalle3Service = dalle3Service;
    }

    @PostMapping
    public ResponseEntity<ImageResponse> generateImage(@RequestBody PromptRequest request) {
        try {
            // (선택) 장르 검증: 뮤지컬/밴드 아니면 400
            if (request.getGenre() != null &&
                    !(request.getGenre().equals("뮤지컬") || request.getGenre().equals("밴드"))) {
                return ResponseEntity.badRequest().build();
            }

            // 1) 프롬프트 생성
            PromptResponse promptResponse = promptService.generatePrompt(request);
            String prompt = promptResponse.getPrompt();

            // 2) 단일 이미지 생성 (항상 1장, 1:1)
            String imageUrl = dalle3Service.generateSingleImageUrl(prompt);

            // 3) 응답
            ImageResponse response = new ImageResponse();
            response.setPrompt(prompt);
            response.setImageUrl(imageUrl);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
