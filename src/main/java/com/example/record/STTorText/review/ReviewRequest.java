package com.example.record.review;

public record ReviewRequest(
        Long transcriptionId,
        String text
) {}
