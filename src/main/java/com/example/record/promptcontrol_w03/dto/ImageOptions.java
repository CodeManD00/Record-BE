package com.example.record.promptcontrol_w03.dto;

import java.util.List;

public class ImageOptions {
    // 텍스트 레벨 옵션
    private String stylePreset;            // e.g., "photorealistic", "illustration", "gothic"
    private List<String> colorPalette;     // e.g., ["deep blue","purple","gold"]
    private String lighting;               // e.g., "cinematic backlight"
    private String mood;                   // e.g., "melancholic"
    private String camera;                 // e.g., "low angle"
    private String composition;            // e.g., "rule of thirds"
    private List<String> negativeTerms;    // e.g., ["text","letters","watermark","logo"]
    private String variationStrategy = "none"; // "none" | "prompt-jitter" (확장 여지)

    // getters/setters
    public String getStylePreset() { return stylePreset; }
    public void setStylePreset(String stylePreset) { this.stylePreset = stylePreset; }

    public List<String> getColorPalette() { return colorPalette; }
    public void setColorPalette(List<String> colorPalette) { this.colorPalette = colorPalette; }

    public String getLighting() { return lighting; }
    public void setLighting(String lighting) { this.lighting = lighting; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public String getCamera() { return camera; }
    public void setCamera(String camera) { this.camera = camera; }

    public String getComposition() { return composition; }
    public void setComposition(String composition) { this.composition = composition; }

    public List<String> getNegativeTerms() { return negativeTerms; }
    public void setNegativeTerms(List<String> negativeTerms) { this.negativeTerms = negativeTerms; }

    public String getVariationStrategy() { return variationStrategy; }
    public void setVariationStrategy(String variationStrategy) { this.variationStrategy = variationStrategy; }
}
