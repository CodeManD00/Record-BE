package com.example.record.promptcontrol_w03.dto;

import java.util.Map;

public class PromptResponse {
    private String prompt;
    private Map<String, Object> meta;

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, Object> meta) {
        this.meta = meta;
    }
}
