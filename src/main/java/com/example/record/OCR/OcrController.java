// OcrController: 이미지 파일을 업로드 받아 OCR 처리 후 텍스트를 반환하는 컨트롤러입니다.

package com.example.record.OCR;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController
@RequestMapping("/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService; // Google Vision API 기반 OCR 처리 서비스

    // POST /ocr: 이미지 업로드 → OCR 처리 → 텍스트 반환
    @PostMapping
    public ResponseEntity<String> uploadImage(@RequestParam MultipartFile file) throws Exception {
        // 업로드된 파일을 임시 파일로 저장
        File tempFile = File.createTempFile("ocr_", file.getOriginalFilename());
        file.transferTo(tempFile);

        // OCR 처리 수행
        String text = ocrService.extractTextFromImage(tempFile);

        // 임시 파일 삭제
        tempFile.delete();

        // 추출된 텍스트 반환
        return ResponseEntity.ok(text);
    }
}
