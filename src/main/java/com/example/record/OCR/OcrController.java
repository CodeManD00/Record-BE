package com.example.record.OCR;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Map;

@RestController
@RequestMapping("/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;   // Google Vision API 기반 OCR
    private final GptClient gptClient;     // GPT 구조화 클라이언트

    private final ObjectMapper om = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * POST /ocr/image : 이미지 업로드 → OCR 텍스트만 반환
     */
    @PostMapping("/image")
    public ResponseEntity<String> uploadImage(@RequestParam MultipartFile file) throws Exception {
        File tempFile = createTempFromMultipart(file);
        try {
            String text = ocrService.extractTextFromImage(tempFile);
            return ResponseEntity.ok(text == null ? "" : text);
        } finally {
            if (tempFile.exists()) tempFile.delete();
        }
    }

    /**
     * POST /ocr/image/structured : OCR → GPT 구조화 → PerformanceInfo 반환
     */
    @PostMapping("/image/structured")
    public ResponseEntity<PerformanceInfo> uploadAndParse(@RequestParam MultipartFile file) throws Exception {
        File tempFile = createTempFromMultipart(file);
        try {
            // 1) OCR
            String text = ocrService.extractTextFromImage(tempFile);

            // 2) 프롬프트 구성
            String prompt = """
                    아래 OCR 텍스트에서 공연 정보를 JSON으로 추출해줘.
                    필드: title, date(YYYY-MM-DD), time(예: 19:00 또는 '오후 7시'), venue, artist.
                    값이 없으면 빈 문자열로 넣어.
                    반드시 순수 JSON만 출력해. 설명/마크다운/코드는 넣지 마.

                    OCR:
                    %s
                    """.formatted(text == null ? "" : text);

            // 3) GPT 호출 → JSON 문자열(혹은 텍스트) 수신
            String json = gptClient.getStructuredJsonFromPrompt(prompt);

            // 4) JSON → DTO 매핑
            //    만약 모델이 문자열 앞/뒤에 코드블록을 붙이면 제거 시도
            String cleaned = stripCodeFence(json).trim();

            // JSON 객체를 바로 PerformanceInfo로 매핑 시도
            PerformanceInfo info;
            try {
                info = om.readValue(cleaned, PerformanceInfo.class);
            } catch (Exception directFail) {
                // 혹시 키가 섞여 있어도 유연 매핑 (Map → DTO)
                Map<String, Object> map = om.readValue(cleaned, new TypeReference<>() {});
                info = new PerformanceInfo(
                        str(map.get("title")),
                        str(map.get("date")),
                        str(map.get("time")),
                        str(map.get("venue")),
                        str(map.get("artist"))
                );
            }

            return ResponseEntity.ok(info);
        } finally {
            if (tempFile.exists()) tempFile.delete();
        }
    }

    // ────────── 유틸 ──────────

    private static File createTempFromMultipart(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드된 파일이 비어 있습니다.");
        }
        String suffix = ".tmp";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            suffix = original.substring(original.lastIndexOf('.')); // ".jpg" 등
        }
        File temp = File.createTempFile("ocr_", suffix);
        file.transferTo(temp);
        return temp;
    }

    private static String stripCodeFence(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.startsWith("```")) {
            int idx = t.indexOf('\n');
            if (idx > 0) t = t.substring(idx + 1);
            int end = t.lastIndexOf("```");
            if (end >= 0) t = t.substring(0, end);
        }
        return t;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
