package com.example.record.promptcontrol_w03.dto;

/**
 * gpt1 이미지 생성 결과 응답 DTO
 * - prompt: 사용자가 입력한 프롬프트
 * - imageUrl: 생성된 이미지의 URL
 * - error: 오류 발생 시 메시지 저장
 */
public class ImageResponse {

    /** 사용자가 입력한 프롬프트 문장 */
    private String prompt;

    /** DALL·E API로 생성된 이미지의 URL */
    private String imageUrl;

    /** 오류 메시지 (정상 생성 시 null) */
    private String error;

    // ─────────── 기본 생성자 ───────────
    public ImageResponse() {}

    // ─────────── Getter / Setter ───────────
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    // ─────────── 에러 응답용 팩토리 메서드 ───────────
    public static ImageResponse error(String message) {
        ImageResponse response = new ImageResponse();
        response.setError(message);
        return response;
    }
}
