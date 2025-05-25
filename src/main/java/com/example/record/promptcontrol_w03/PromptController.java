package com.example.record.promptcontrol_w03;

import com.example.record.promptcontrol_w03.dto.PromptRequest;
import com.example.record.promptcontrol_w03.dto.PromptResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/prompt")
public class PromptController {

    private final PromptService promptService;

    public PromptController(PromptService promptService) {
        this.promptService = promptService;
    }

    @PostMapping
    public ResponseEntity<PromptResponse> generatePrompt(@RequestBody PromptRequest request) {
        PromptResponse response = promptService.generatePrompt(request);
        return ResponseEntity.ok(response);
    }
}
