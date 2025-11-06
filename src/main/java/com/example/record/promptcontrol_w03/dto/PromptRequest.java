package com.example.record.promptcontrol_w03.dto;
/*
클라이언트(프론트엔드)에서 들어오는 프롬프트 요청 데이터를 담는 요청 DTO

예를 들어 사용자가 “이 공연 후기 요약해줘” 같은 요청을 보낼 때,
이 클래스가 해당 텍스트(prompt, genre, summaryType, 등)를 받음.
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
    private String imageRequest; // 예: "푸른 색 기반, 귀여운 그림체"
    private String size;         // 예: "1024x1024"
    private int n = 1;           // 생성 장수 (기본 1)

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
}
