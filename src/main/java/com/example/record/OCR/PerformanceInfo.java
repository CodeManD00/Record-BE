// PerformanceInfo: OCR 또는 GPT 결과에서 추출한 공연의 핵심 정보를 담는 DTO 클래스입니다.

package com.example.record.OCR;

public class PerformanceInfo {

    private String title;   // 공연 제목
    private String date;    // 공연 날짜 (예: 2025-08-10)
    private String time;    // 공연 시간 (예: 오후 7시)
    private String venue;   // 공연 장소 (예: 예술의전당 오페라극장)
    private String artist;  // 공연 아티스트 또는 출연자 이름

    // 기본 생성자
    public PerformanceInfo() {}

    // 전체 필드를 받는 생성자
    public PerformanceInfo(String title, String date, String time, String venue, String artist) {
        this.title = title;
        this.date = date;
        this.time = time;
        this.venue = venue;
        this.artist = artist;
    }

    // getter/setter
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }
    public void setTime(String time) {
        this.time = time;
    }

    public String getVenue() {
        return venue;
    }
    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getArtist() {
        return artist;
    }
    public void setArtist(String artist) {
        this.artist = artist;
    }
}
