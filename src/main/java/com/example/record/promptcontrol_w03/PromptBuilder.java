package com.example.record.promptcontrol_w03;

import com.example.record.promptcontrol_w03.dto.ImageOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PromptBuilder {

    private static final String NO_TEXT_RULE =
            "No captions, no letters, no words, no logos, no watermarks.";

    private static final List<String> JITTER_LIGHTS = List.of(
            "cinematic backlight", "soft ambient light", "dramatic spotlight", "neon rim light"
    );

    /** basePrompt에 옵션(스타일/팔레트/무드/조명)을 덧입힘 */
    public static String applyOptions(String basePrompt, ImageOptions opt, int variantIndex) {
        if (opt == null) {
            return basePrompt.trim() + "\n" + NO_TEXT_RULE;
        }

        StringBuilder sb = new StringBuilder(basePrompt.trim());

        // variation 전략
        String lighting = opt.getLighting();
        if ("prompt-jitter".equalsIgnoreCase(opt.getVariationStrategy())) {
            Random r = new Random(System.nanoTime() + variantIndex);
            if (lighting == null) lighting = JITTER_LIGHTS.get(r.nextInt(JITTER_LIGHTS.size()));
        }

        List<String> tags = new ArrayList<>();

        if (opt.getStylePreset() != null) tags.add(opt.getStylePreset());
        if (opt.getMood() != null) tags.add(opt.getMood());
        if (lighting != null) tags.add(lighting);

        if (opt.getColorPalette() != null && !opt.getColorPalette().isEmpty()) {
            tags.add("color palette: " + String.join(", ", opt.getColorPalette()));
        }

        if (!tags.isEmpty()) {
            sb.append("\nStyle & constraints: ").append(String.join(", ", tags)).append(".");
        }

        // 항상 텍스트 금지
        sb.append("\n").append(NO_TEXT_RULE);

        return sb.toString();
    }
}
