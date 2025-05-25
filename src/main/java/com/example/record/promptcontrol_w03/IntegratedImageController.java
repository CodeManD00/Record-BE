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
        PromptResponse promptResponse = promptService.generatePrompt(request);
        String prompt = promptResponse.getPrompt();

        String imageUrl = dalle3Service.generateImageUrlOnly(prompt); // 이미지 URL 반환용 메서드로 되어 있어야 함

        ImageResponse response = new ImageResponse();
        response.setPrompt(prompt);
        response.setImageUrl(imageUrl);
        return ResponseEntity.ok(response);
    }
}
