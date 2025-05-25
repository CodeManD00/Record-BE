package com.example.record.promptcontrol_w03;

import com.example.record.promptcontrol_w03.dto.PromptRequest;
import com.example.record.promptcontrol_w03.dto.PromptResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PromptService {
    public PromptResponse generatePrompt(PromptRequest input) {
        String genre = input.getGenre();
        String prompt;

        if ("뮤지컬".equals(genre)) {
            prompt = generateMusicalPrompt(input);
        } else if ("밴드".equals(genre)) {
            prompt = generateBandPrompt(input);
        } else {
            throw new IllegalArgumentException("지원하지 않는 장르입니다: " + genre);
        }

        PromptResponse response = new PromptResponse();
        response.setPrompt(prompt);
        Map<String, Object> meta = new HashMap<>();
        meta.put("structure", genre);
        meta.put("style", "gothic");
        meta.put("tone", "emotional");
        meta.put("inferred_keywords", new String[]{"obsession", "conflict"});
        response.setMeta(meta);

        return response;
    }

    private String generateMusicalPrompt(PromptRequest input) {
        String cast = String.join(", ", input.getCast());
        String theme = extractTheme(input.getTitle(), input.getReview());

        return String.format(
                "A deeply emotional musical theater scene about %s,\n" +
                        "set in early 20th century Europe and depicting tragic relationships,\n" +
                        "featuring intense character interactions,\n" +
                        "with actors portraying conflict and vulnerability,\n" +
                        "under dramatic spotlight and heavy shadows,\n" +
                        "performed at %s on %s,\n" +
                        "starring %s.\n" +
                        "Inspired by *%s*'s gothic aesthetic.\n" +
                        "No text, no captions, no letters, no words in the image.",
                theme, input.getLocation(), input.getDate(), cast, input.getTitle()
        );
    }

    private String generateBandPrompt(PromptRequest input) {
        return String.format(
                "A moody alternative rock live performance scene by %s,\n" +
                        "featuring a powerful set with emotional lyrics,\n" +
                        "set during autumn,\n" +
                        "at %s on %s,\n" +
                        "with a stage design inspired by %s's concert visuals,\n" +
                        "including deep blue and purple lighting, fog machines and backlights,\n" +
                        "without showing any characters or text.\n" +
                        "No captions, no letters, no words in the image.",
                input.getTitle(), input.getLocation(), input.getDate(), input.getTitle()
        );
    }

    private String extractTheme(String title, String review) {
        return "obsession and destructive love"; // TODO: replace with GPT or namuwiki
    }
}