package com.example.record.promptcontrol_w03.dto;

import java.util.List;

public class PromptRequest {
    private String title;
    private String location;
    private String date;
    private String genre;
    private List<String> cast;
    private String review;

    // ✅ Getter 메서드들 추가
    public String getTitle() {
        return title;
    }

    public String getLocation() {
        return location;
    }

    public String getDate() {
        return date;
    }

    public String getGenre() {
        return genre;
    }

    public List<String> getCast() {
        return cast;
    }

    public String getReview() {
        return review;
    }

    // ❗ 만약 setter도 필요하면 아래처럼 추가 가능
    public void setTitle(String title) {
        this.title = title;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public void setCast(List<String> cast) {
        this.cast = cast;
    }

    public void setReview(String review) {
        this.review = review;
    }
}

