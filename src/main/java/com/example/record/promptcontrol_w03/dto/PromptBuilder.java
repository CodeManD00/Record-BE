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
    private static final List<String> JITTER_CAMERAS = List.of(
            "wide-angle", "telephoto", "low angle", "overhead shot", "close-up"
    );
    private static final List<String> JITTER_COMPOSITIONS = List.of(
            "rule of thirds", "centered subject", "leading lines", "symmetrical framing"
    );

    /** basePrompt에 옵션(스타일/팔레트/무드/카메라/구도/네거티브)을 덧입힘 */
    public static String applyOptions(String basePrompt, ImageOptions opt, int variantIndex) {
        if (opt == null) {
            return basePrompt.trim() + "\n" + NO_TEXT_RULE;
        }

        StringBuilder sb = new StringBuilder(basePrompt.trim());

        // variation 전략 (간단 샘플: prompt-jitter면 일부를 샘플링)
        String lighting = opt.getLighting();
        String camera = opt.getCamera();
        String composition = opt.getComposition();

        if ("prompt-jitter".equalsIgnoreCase(opt.getVariationStrategy())) {
            Random r = new Random(System.nanoTime() + variantIndex);
            if (lighting == null) lighting = JITTER_LIGHTS.get(r.nextInt(JITTER_LIGHTS.size()));
            if (camera == null) camera = JITTER_CAMERAS.get(r.nextInt(JITTER_CAMERAS.size()));
            if (composition == null) composition = JITTER_COMPOSITIONS.get(r.nextInt(JITTER_COMPOSITIONS.size()));
        }

        List<String> tags = new ArrayList<>();

        if (opt.getStylePreset() != null) tags.add(opt.getStylePreset());
        if (opt.getMood() != null) tags.add(opt.getMood());
        if (lighting != null) tags.add(lighting);
        if (camera != null) tags.add(camera);
        if (composition != null) tags.add(composition);

        if (opt.getColorPalette() != null && !opt.getColorPalette().isEmpty()) {
            tags.add("color palette: " + String.join(", ", opt.getColorPalette()));
        }

        if (!tags.isEmpty()) {
            sb.append("\nStyle & constraints: ").append(String.join(", ", tags)).append(".");
        }

        // 항상 텍스트 금지
        sb.append("\n").append(NO_TEXT_RULE);

        // 추가 네거티브
        if (opt.getNegativeTerms() != null && !opt.getNegativeTerms().isEmpty()) {
            sb.append("\nAvoid: ").append(String.join(", ", opt.getNegativeTerms())).append(".");
        }

        return sb.toString();
    }
}
