package com.example.record.promptcontrol_w03.dto;

public class ImageResponse {
    private String prompt;
    private String imageUrl;

    // ✅ 추가할 부분
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    // 필요한 경우 getter도 함께 추가
    public String getPrompt() {
        return prompt;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
