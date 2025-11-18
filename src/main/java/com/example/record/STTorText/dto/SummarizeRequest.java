// com/example/record/STT/SummarizeRequest.java
package com.example.record.STTorText.dto;

public record SummarizeRequest(
        Long transcriptionId,  // 선택
        String rawText         // 선택
) {}
