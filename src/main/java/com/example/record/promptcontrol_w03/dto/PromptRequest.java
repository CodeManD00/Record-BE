package com.example.record.promptcontrol_w03.dto;
/*
프론트엔드가 /prompt 또는 /generate-image 호출 시 전달하는 요청 DTO.
imageRequest 는 사용자가 직접 입력하는 추가 스타일/요청이며,
재생성 모드에서는 basePrompt(기존 프롬프트)를 보내면 분석 없이 덧붙여 사용한다.
*/
import java.util.List;

public class PromptRequest {

    private String title;
    private String location;
    private String date;
    private String genre;
    private List<String> cast;
    private String review;

    // 🎨 이미지 프롬프트 관련
    private String imageRequest;   // 예: "푸른 색 기반, 귀여운 그림체"
    private String size;           // 예: "1024x1024"
    private int n = 1;             // 생성 장수 (기본 1)

    // ♻️ 재생성 모드 지원 (선택)
    private String basePrompt;     // 기존 프롬프트 그대로 + imageRequest 덧붙여서 사용

    // ===== Getters =====
    public String getTitle() { return title; }
    public String getLocation() { return location; }
    public String getDate() { return date; }
    public String getGenre() { return genre; }
    public List<String> getCast() { return cast; }
    public String getReview() { return review; }
    public String getImageRequest() { return imageRequest; }
    public String getSize() { return size; }
    public int getN() { return n; }
    public String getBasePrompt() { return basePrompt; }

    // ===== Setters =====
    public void setTitle(String title) { this.title = title; }
    public void setLocation(String location) { this.location = location; }
    public void setDate(String date) { this.date = date; }
    public void setGenre(String genre) { this.genre = genre; }
    public void setCast(List<String> cast) { this.cast = cast; }
    public void setReview(String review) { this.review = review; }
    public void setImageRequest(String imageRequest) { this.imageRequest = imageRequest; }
    public void setSize(String size) { this.size = size; }
    public void setN(int n) { this.n = n; }
    public void setBasePrompt(String basePrompt) { this.basePrompt = basePrompt; }
}
