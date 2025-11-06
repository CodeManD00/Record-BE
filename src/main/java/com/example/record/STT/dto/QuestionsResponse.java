// com/example/record/STT/QuestionsResponse.java
package com.example.record.STT.dto;

import java.util.List;

public record QuestionsResponse(
        Long transcriptionId,
        List<String> questions
) {}
