// GptResponse: GPT로부터 받은 요약(summary)과 질문(question)을 담는 응답 레코드입니다.

package com.example.record.STT;

public record GptResponse(String summary, String question) {}
