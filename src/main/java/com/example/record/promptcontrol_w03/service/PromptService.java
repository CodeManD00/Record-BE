package com.example.record.promptcontrol_w03.service;

/*
역할: “이미지용 짧은 영어 프롬프트”를 만들어 주는 핵심 서비스.
핵심 기능
장르 분기: 뮤지컬/밴드 케이스로 프롬프트 템플릿 분리 (미지원 장르면 예외)
공연 DB 연계:
MusicalDbRepository에서 작품/캐릭터 조회(요약, 배경, 주요 인물수, 캐릭터 속성 활용)
BandDbRepository에서 밴드명/의미/상징/포스터 색 등 조회
리뷰 내용 분석 연계: ReviewAnalysisService.analyzeReview(review) 호출 → 감정/주제/배경/조명/행동/캐릭터 등 JSON 추출
영문화/정규화: 한국어 키워드를 영어로 치환(감정/관계/나이/성별/장소/시대 등 광범위 매핑)
2~3문장 압축: OpenAIChatService를 사용해 자연스러운 2~3문장으로 요약 + imageRequest 녹여 넣기
길이 가드: 문장 단위로 최대 글자 수를 넘지 않게 안전절단
결과: PromptResponse(prompt, meta) 생성 (meta에는 장르/요약여부/추론 키워드 등)
보조 메서드: 캐릭터 설명 정리(JSON 느낌 문자열 → 자연어), 영어 치환, 문장단위 클램프 등
 */
import com.example.record.band.BandDb;
import com.example.record.band.BandDbRepository;
import com.example.record.musical.MusicalCharacter;
import com.example.record.musical.MusicalDb;
import com.example.record.musical.MusicalDbRepository;
import com.example.record.promptcontrol_w03.dto.PromptRequest;
import com.example.record.promptcontrol_w03.dto.PromptResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PromptService {

    private final ReviewAnalysisService reviewAnalysisService;
    private final MusicalDbRepository musicalDbRepository;
    private final BandDbRepository bandDbRepository;
    private final OpenAIChatService openAIChatService;

    /** 최종 압축 프롬프트 길이 상한(문장 경계 기반) */
    @Value("${openai.limits.imagePromptMaxChars:900}")
    private int imagePromptMaxChars;

    // ─────────────────────────────────────────────────────────────────────
    // 공개 메서드: 최종 이미지 프롬프트 생성 (항상 2~3문장, 영어)
    // ─────────────────────────────────────────────────────────────────────
    public PromptResponse generatePrompt(PromptRequest input) {
        final String genre = input.getGenre();

        // 1) basePrompt 생성 (DB/후기 분석 반영)
        final String basePrompt = switch (genre) {
            case "뮤지컬" -> generateMusicalPrompt(input);
            case "밴드"   -> generateBandPrompt(input);
            default       -> throw new IllegalArgumentException("지원하지 않는 장르입니다: " + genre);
        };

        // 2) 2~3문장 압축 (imageRequest를 자연스럽게 녹임)
        final String shortForm = compressToTwoOrThreeSentences(basePrompt, safe(input.getImageRequest()));

        // 3) 문장 경계 기반 길이 가드
        final String finalPrompt = clampBySentence(shortForm, imagePromptMaxChars);

        // 4) 응답 메타 포함
        PromptResponse response = new PromptResponse();
        response.setPrompt(finalPrompt);

        Map<String, Object> meta = new HashMap<>();
        meta.put("structure", genre);
        meta.put("shortForm", true);
        meta.put("imageRequest", safe(input.getImageRequest()));
        meta.put("inferred_keywords", new String[]{"visual", "mood", "scene"});
        response.setMeta(meta);

        return response;
    }

    // ─────────────────────────────────────────────────────────────────────
    // 2~3문장 압축 (OpenAIChatService 사용)
    // ─────────────────────────────────────────────────────────────────────
    /**
     * basePrompt(멀티라인 가능) + imageRequest(색/스타일/구도 등)를
     * 영어 2~3문장으로 압축. 규칙/라벨/불릿/개행 없이 자연스러운 산문으로.
     */
    private String compressToTwoOrThreeSentences(String basePrompt, String imageRequest) {
        String userMsg = (imageRequest == null || imageRequest.isBlank())
                ? "Base prompt:\n" + basePrompt
                : "Base prompt:\n" + basePrompt + "\n\nAdditional style requests:\n" + imageRequest;

        String result = openAIChatService.complete(
                // system
                """
                You rewrite rich scene prompts for text-to-image models.
                Requirements:
                - Output MUST be in ENGLISH.
                - Output MUST be exactly 2 or 3 sentences. No bullet points, no numbered lists, no line breaks.
                - Preserve concrete visual details: subjects, setting, mood, composition, lighting, color cues.
                - If additional style requests are given, subtly weave them into the prose.
                - Include naturally that there is no visible text/logos/watermarks in the image (do not list rules).
                - Avoid meta language like "the prompt is" or quotes. Write pure descriptive prose only.
                """,
                // user
                userMsg
        );

        return result == null ? "" : result.trim();
    }

    // ─────────────────────────────────────────────────────────────────────
    // 유틸
    // ─────────────────────────────────────────────────────────────────────
    /** 공백/널 안전화 */
    private static String safe(String s) {
        return (s == null) ? null : s.trim();
    }

    /**
     * 문장 경계 기반 길이 가드:
     * - 1차: 전체가 max 이하면 그대로
     * - 2차: . ! ? 단위로 자르며 누적 길이 ≤ max 유지
     * - 3차: 그래도 초과시 하드 컷
     */
    private String clampBySentence(String text, int max) {
        if (text == null) return null;
        if (text.length() <= max) return text;

        String[] parts = text.split("(?<=[.!?])\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() + p.length() + 1 > max) break;
            if (sb.length() > 0) sb.append(' ');
            sb.append(p);
        }
        if (sb.length() > 0) return sb.toString();

        return text.substring(0, Math.min(text.length(), max));
    }

    /**
     * 캐릭터 설명에서 JSON 형식을 제거하고 자연어로 변환
     */
    private String cleanCharacterDescription(String description) {
        if (description == null || description.trim().isEmpty()) return "";
        description = description.trim();

        if (description.startsWith("{") && description.contains("name=")) {
            int nameStart = description.indexOf("name=");
            if (nameStart >= 0) {
                int nameEnd = description.indexOf(",", nameStart);
                if (nameEnd == -1) nameEnd = description.indexOf("}", nameStart);
                if (nameEnd > nameStart) {
                    String name = description.substring(nameStart + 5, nameEnd).trim();
                    int descStart = description.indexOf("description=");
                    if (descStart >= 0) {
                        int descEnd = description.indexOf(",", descStart);
                        if (descEnd == -1) descEnd = description.indexOf("}", descStart);
                        if (descEnd > descStart) {
                            String desc = description.substring(descStart + 11, descEnd).trim();
                            return name + (!desc.isEmpty() ? " (" + desc + ")" : "");
                        }
                    }
                    return name;
                }
            }
        }
        return description;
    }

    /**
     * 한국어를 영어로 단순 매핑(프롬프트 간결화 목적)
     */
    private String translateToEnglish(String korean) {
        if (korean == null || korean.trim().isEmpty()) return "unknown";
        if (!korean.matches(".*[가-힣]+.*")) return korean.trim();

        // 감정
        korean = korean.replace("아쉬움", "regret")
                .replace("답답함", "frustration")
                .replace("분노", "anger")
                .replace("만족", "satisfaction")
                .replace("기쁨", "joy")
                .replace("슬픔", "sadness")
                .replace("사랑", "love")
                .replace("증오", "hatred")
                .replace("감동적", "emotional")
                .replace("긴장", "tension")
                .replace("갈등", "conflict")
                .replace("여운", "lingering emotion")
                .replace("놀람", "surprise")
                .replace("아리함", "confusion")
                .replace("깊은", "deep");

        // 장르/설정
        korean = korean.replace("뮤지컬", "musical")
                .replace("밴드", "band")
                .replace("콘서트", "concert")
                .replace("극장", "theater")
                .replace("무대", "stage")
                .replace("호텔", "hotel")
                .replace("방", "room")
                .replace("일제강점기", "Japanese colonial period")
                .replace("의", " of")
                .replace("은유", "metaphor")
                .replace("창작", "creation")
                .replace("추락", "fall")
                .replace("현실", "reality")
                .replace("허상", "illusion")
                .replace("예술", "art")
                .replace("본질", "essence")
                .replace("인간", "human")
                .replace("존엄", "dignity")
                .replace("납치", "abduction");

        // 나이/성별
        korean = korean.replace("20대 중반", "mid-20s")
                .replace("20대 초중반", "early to mid-20s")
                .replace("20대 초반", "early 20s")
                .replace("20대 후반", "late 20s")
                .replace("30대", "30s")
                .replace("40대", "40s")
                .replace("50대", "50s")
                .replace("남성", "male")
                .replace("여성", "female")
                .replace("남자", "male")
                .replace("여자", "female");

        // 관계
        korean = korean.replace("연인", "lovers")
                .replace("친구", "friends")
                .replace("가족", "family")
                .replace("동료", "colleagues");

        // 액션
        korean = korean.replace("노래", "singing")
                .replace("춤", "dancing")
                .replace("연기", "acting")
                .replace("연주", "playing")
                .replace("공연", "performance");

        // 직업/역할
        korean = korean.replace("시인", "poet")
                .replace("건축가", "architect")
                .replace("기생", "gisaeng")
                .replace("배우", "actor")
                .replace("가수", "singer")
                .replace("댄서", "dancer");

        // 조명
        korean = korean.replace("어둠", "darkness")
                .replace("밝음", "brightness")
                .replace("무대조명", "stage lighting")
                .replace("스포트라이트", "spotlight");

        // 남은 한글 제거 → 공백 정리
        korean = korean.replaceAll("[가-힣]", " ");
        korean = korean.replaceAll("\\s+", " ").trim();

        return korean.isEmpty() ? "unknown" : korean;
    }

    // ─────────────────────────────────────────────────────────────────────
    // DB 기반 프롬프트 생성 (기존 로직 유지, 서술만 생성)
    // ─────────────────────────────────────────────────────────────────────
    private String generateMusicalPrompt(PromptRequest input) {
        String normalizedTitle = "";
        if (input.getTitle() != null) {
            normalizedTitle = input.getTitle()
                    .trim()
                    .replaceAll("\\s+", "")
                    .replaceAll("[\\u00A0\\u2000-\\u200B\\u2028\\u2029\\uFEFF]", "");
        }

        Optional<MusicalDb> musicalOpt = musicalDbRepository.findByTitle(normalizedTitle);
        if (!musicalOpt.isPresent() && input.getTitle() != null) {
            String originalTitle = input.getTitle().trim();
            musicalOpt = musicalDbRepository.findByTitle(originalTitle);
        }
        if (!musicalOpt.isPresent()) {
            List<MusicalDb> musicals = musicalDbRepository.findByTitleContaining(normalizedTitle);
            if (!musicals.isEmpty()) {
                musicalOpt = Optional.of(musicals.get(0));
            }
        }
        if (!musicalOpt.isPresent() && input.getTitle() != null) {
            String originalTitle = input.getTitle().trim();
            List<MusicalDb> musicals = musicalDbRepository.findByTitleContaining(originalTitle);
            if (!musicals.isEmpty()) {
                musicalOpt = Optional.of(musicals.get(0));
            }
        }

        Optional<MusicalDb> musicalWithCharactersOpt = Optional.empty();
        if (musicalOpt.isPresent()) {
            MusicalDb musical = musicalOpt.get();
            Long musicalId = musical.getId();
            musicalWithCharactersOpt = musicalDbRepository.findByIdWithCharacters(musicalId);
        }

        Map<String, Object> data = reviewAnalysisService.analyzeReview(input.getReview());

        if (musicalWithCharactersOpt.isPresent()) {
            MusicalDb musical = musicalWithCharactersOpt.get();
            List<MusicalCharacter> characters = musical.getCharacters();

            String musicalSummary = musical.getSummary() != null ? musical.getSummary() : (String) data.get("theme");
            String musicalBackground = musical.getBackground() != null ? musical.getBackground() : (String) data.get("setting");
            Integer characterCount = musical.getMainCharacterCount() != null
                    ? musical.getMainCharacterCount()
                    : (characters != null && !characters.isEmpty() ? characters.size() : 3);

            StringBuilder characterDetails = new StringBuilder();
            if (characters != null && !characters.isEmpty()) {
                int maxCharacters = Math.min(characters.size(), Math.min(characterCount, 5));
                for (int i = 0; i < maxCharacters; i++) {
                    MusicalCharacter character = characters.get(i);
                    if (i > 0) characterDetails.append(", ");

                    String charInfo = character.getName();
                    StringBuilder charAttributes = new StringBuilder();
                    if (character.getAge() != null && !character.getAge().trim().isEmpty()) {
                        charAttributes.append(translateToEnglish(character.getAge()));
                    }
                    if (character.getGender() != null && !character.getGender().trim().isEmpty()) {
                        if (charAttributes.length() > 0) charAttributes.append(" ");
                        charAttributes.append(translateToEnglish(character.getGender()));
                    }
                    if (character.getOccupation() != null && !character.getOccupation().trim().isEmpty()) {
                        if (charAttributes.length() > 0) charAttributes.append(" ");
                        charAttributes.append(translateToEnglish(character.getOccupation()));
                    }
                    if (character.getDescription() != null && !character.getDescription().trim().isEmpty()) {
                        if (charAttributes.length() > 0) charAttributes.append(", ");
                        charAttributes.append(translateToEnglish(character.getDescription()));
                    }
                    if (charAttributes.length() > 0) {
                        charInfo += " (a " + charAttributes + ")";
                    }
                    characterDetails.append(charInfo);
                }
            } else {
                characterDetails.append(characterCount).append(" distinct characters");
            }

            return String.format(
                    "A %s musical theater scene about %s, set in %s and depicting %s, featuring exactly %d characters only: %s. " +
                            "The scene must include exactly %d characters—no extras or background people. With %s, under %s. " +
                            "There is no visible text, letters, words, captions, logos, or watermarks in the image.",
                    translateToEnglish((String) data.get("emotion")),
                    translateToEnglish(musicalSummary),
                    translateToEnglish(musicalBackground),
                    translateToEnglish((String) data.get("relationship")),
                    characterCount,
                    translateToEnglish(characterDetails.toString()),
                    characterCount,
                    translateToEnglish((String) data.get("actions")),
                    translateToEnglish((String) data.get("lighting"))
            );
        }

        // DB 정보가 없을 때: 후기 분석만 사용
        String musicalSummary = (String) data.get("theme");
        String musicalBackground = (String) data.get("setting");

        StringBuilder characterPart = new StringBuilder();
        String cleanChar1 = cleanCharacterDescription(Objects.toString(data.get("character1"), ""));
        String cleanChar2 = cleanCharacterDescription(Objects.toString(data.get("character2"), ""));
        if (!cleanChar1.isEmpty() && !cleanChar2.isEmpty()) {
            characterPart.append(cleanChar1).append(" and ").append(cleanChar2);
        } else if (!cleanChar1.isEmpty()) {
            characterPart.append(cleanChar1);
        } else if (!cleanChar2.isEmpty()) {
            characterPart.append(cleanChar2);
        }
        for (int i = 3; i <= 5; i++) {
            String key = "character" + i;
            if (data.containsKey(key)) {
                String cleanChar = cleanCharacterDescription(Objects.toString(data.get(key), ""));
                if (!cleanChar.isEmpty()) {
                    if (characterPart.length() > 0) characterPart.append(", and ");
                    characterPart.append(cleanChar);
                }
            }
        }
        if (characterPart.length() == 0) {
            characterPart.append("the main characters");
        }

        return String.format(
                "A %s musical theater scene about %s, set in %s and depicting %s, featuring %s. " +
                        "With %s, under %s. There is no visible text, letters, words, captions, logos, or watermarks in the image.",
                translateToEnglish((String) data.get("emotion")),
                translateToEnglish(musicalSummary != null ? musicalSummary : ""),
                translateToEnglish(musicalBackground != null ? musicalBackground : ""),
                translateToEnglish((String) data.get("relationship") != null ? (String) data.get("relationship") : ""),
                translateToEnglish(characterPart.toString()),
                translateToEnglish((String) data.get("actions") != null ? (String) data.get("actions") : ""),
                translateToEnglish((String) data.get("lighting") != null ? (String) data.get("lighting") : "")
        );
    }

    private String generateBandPrompt(PromptRequest input) {
        Optional<BandDb> bandOpt = bandDbRepository.findByBandNameIgnoreCase(input.getTitle());

        String bandName = input.getTitle();
        String bandNameMeaning = bandOpt.map(BandDb::getBandNameMeaning)
                .orElse("emotional and powerful music");
        String posterColor = bandOpt.map(BandDb::getPosterColor)
                .orElse("deep blue and purple");
        String bandSymbol = bandOpt.map(BandDb::getBandSymbol)
                .orElse("stage design");

        return String.format(
                "A moody alternative rock live performance scene by %s, featuring %s, set during autumn, at %s on %s, " +
                        "with a stage design inspired by %s, including %s lighting, fog machines and backlights. " +
                        "No characters or visible text, letters, words, captions, logos, or watermarks appear in the image.",
                translateToEnglish(bandName),
                translateToEnglish(bandNameMeaning),
                translateToEnglish(input.getLocation()),
                input.getDate(),
                translateToEnglish(bandSymbol),
                translateToEnglish(posterColor)
        );
    }
}
