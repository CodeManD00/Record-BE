package com.example.record.promptcontrol_w03;

import com.example.record.band.BandDb;
import com.example.record.band.BandDbRepository;
import com.example.record.musical.MusicalCharacter;
import com.example.record.musical.MusicalDb;
import com.example.record.musical.MusicalDbRepository;
import com.example.record.promptcontrol_w03.dto.PromptRequest;
import com.example.record.promptcontrol_w03.dto.PromptResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PromptService {

    private final ReviewAnalysisService reviewAnalysisService;
    private final MusicalDbRepository musicalDbRepository;
    private final BandDbRepository bandDbRepository;

    /**
     * PromptService 생성자
     * 
     * 변경 사항:
     * - MusicalDbRepository와 BandDbRepository 의존성 추가
     * - 이유: DB에서 뮤지컬/밴드 정보를 조회해서 프롬프트에 반영하기 때문
     */
    public PromptService(ReviewAnalysisService reviewAnalysisService,
                        MusicalDbRepository musicalDbRepository,
                        BandDbRepository bandDbRepository) {
        this.reviewAnalysisService = reviewAnalysisService;
        this.musicalDbRepository = musicalDbRepository;
        this.bandDbRepository = bandDbRepository;
    }

    private static String clamp(String s, int max) {
        if (s == null) return null;
        return (s.length() <= max) ? s : s.substring(0, max);
    }

    /**
     * 한국어를 영어로 번역하는 간단한 매핑 메서드
     * 
     * 변경 사항:
     * - DALL-E 3는 영어 프롬프트만 지원하므로 한국어를 영어로 번역
     * - 주요 감정, 장르, 설정 등을 영어로 매핑
     * - 복잡한 문장이나 특정 단어가 섞여 있을 경우를 위한 추가 매핑 추가
     * 
     * @param korean 한국어 텍스트 (한-영 혼합 가능)
     * @return 영어로 번역된 텍스트
     */
    private String translateToEnglish(String korean) {
        if (korean == null || korean.trim().isEmpty()) return "unknown";
        
        // 이미 영어만 있으면 그대로 반환 (성능 최적화)
        if (!korean.matches(".*[가-힣]+.*")) {
            return korean.trim();
        }
        
        // 감정 번역 (더 많은 표현 추가)
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
        
        // 장르/설정 번역 (더 많은 표현 추가)
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
        
        // 관계 번역
        korean = korean.replace("연인", "lovers")
                      .replace("친구", "friends")
                      .replace("가족", "family")
                      .replace("동료", "colleagues");
        
        // 액션 번역
        korean = korean.replace("노래", "singing")
                      .replace("춤", "dancing")
                      .replace("연기", "acting")
                      .replace("연주", "playing")
                      .replace("공연", "performance");
        
        // 조명 번역
        korean = korean.replace("어둠", "darkness")
                      .replace("밝음", "brightness")
                      .replace("무대조명", "stage lighting")
                      .replace("스포트라이트", "spotlight");
        
        // 남은 한글 문자는 공백으로 치환 (영어만 남기기)
        korean = korean.replaceAll("[가-힣]", " ");
        
        // 연속된 공백을 하나로 정리하고 앞뒤 공백 제거
        korean = korean.replaceAll("\\s+", " ").trim();
        
        // 결과가 비어있으면 기본값 반환
        return korean.isEmpty() ? "unknown" : korean;
    }

    public PromptResponse generatePrompt(PromptRequest input) {
        String genre = input.getGenre();
        String basePrompt;

        if ("뮤지컬".equals(genre)) {
            basePrompt = generateMusicalPrompt(input);
        } else if ("밴드".equals(genre)) {
            basePrompt = generateBandPrompt(input);
        } else {
            throw new IllegalArgumentException("지원하지 않는 장르입니다: " + genre);
        }

        // 옵션 후처리 적용 (variantIndex=0)
        String afterOptions = PromptBuilder.applyOptions(basePrompt, input.getOptions(), 0);

        // ✅ 길이 관리 (과도하게 길면 품질 저하 방지)
        String finalPrompt = clamp(afterOptions, 1400);

        PromptResponse response = new PromptResponse();
        response.setPrompt(finalPrompt);

        Map<String, Object> meta = new HashMap<>();
        meta.put("structure", genre);
        meta.put("style", "gothic");
        meta.put("tone", "emotional");
        meta.put("inferred_keywords", new String[] { "obsession", "conflict" });
        response.setMeta(meta);

        return response;
    }

    /**
     * 뮤지컬 프롬프트 생성 (DB 정보 반영)
     * 
     * 변경 사항:
     * - DB에서 뮤지컬 정보를 조회해서 프롬프트에 반영
     * - 뮤지컬 제목, 요약, 캐릭터 수, 배경 정보 활용
     * - DB에 정보가 없어도 후기 분석으로 대체
     * 
     * @param input 프롬프트 요청 정보
     * @return 뮤지컬 프롬프트
     */
    private String generateMusicalPrompt(PromptRequest input) {
        // 1. DB에서 뮤지컬 정보 조회
        Optional<MusicalDb> musicalOpt = musicalDbRepository.findByTitleWithCharacters(input.getTitle());
        
        Map<String, Object> data = reviewAnalysisService.analyzeReview(input.getReview());
        
        // 2. DB 정보가 있으면 활용, 없으면 후기 분석 결과 사용
        String musicalTitle = input.getTitle();
        String musicalSummary = musicalOpt.map(MusicalDb::getSummary).orElse((String) data.get("theme"));
        String musicalBackground = musicalOpt.map(MusicalDb::getBackground).orElse((String) data.get("setting"));
        Integer characterCount = musicalOpt.map(MusicalDb::getMainCharacterCount).orElse(2);
        
        // 3. 캐릭터 정보 구성
        String characterPart;
        if (musicalOpt.isPresent() && !musicalOpt.get().getCharacters().isEmpty()) {
            // DB에 캐릭터 정보가 있으면 사용
            List<MusicalCharacter> characters = musicalOpt.get().getCharacters();
            characterPart = characters.get(0).getName();
            for (int i = 1; i < Math.min(characters.size(), 5); i++) {
                characterPart += ", and " + characters.get(i).getName();
            }
        } else {
            // DB에 캐릭터 정보가 없으면 후기 분석 결과 사용
            characterPart = String.format("%s and %s", data.get("character1"), data.get("character2"));
            for (int i = 3; i <= 5; i++) {
                String key = "character" + i;
                if (data.containsKey(key)) characterPart += ", and " + data.get(key);
            }
        }

        // 4. 프롬프트 생성 (DB 정보 우선 사용) - 영어로 번역
        return String.format(
                "A %s musical theater scene about %s,\n" +
                "set in %s and depicting %s,\n" +
                "featuring %s,\n" +
                "with %s,\n" +
                "under %s.\n" +
                "No captions, no letters, no words, no logos, no watermarks.",
                translateToEnglish((String) data.get("emotion")),
                translateToEnglish(musicalSummary),  // DB 요약 또는 후기 분석 결과
                translateToEnglish(musicalBackground),  // DB 배경 또는 후기 분석 결과
                translateToEnglish((String) data.get("relationship")),
                translateToEnglish(characterPart),
                translateToEnglish((String) data.get("actions")),
                translateToEnglish((String) data.get("lighting"))
        );
    }

    /**
     * 밴드 프롬프트 생성 (DB 정보 반영)
     * 
     * 변경 사항:
     * - DB에서 밴드 정보를 조회해서 프롬프트에 반영
     * - 밴드 이름, 의미, 색상, 상징 정보 활용
     * - DB에 정보가 없어도 기본 정보로 대체
     * 
     * @param input 프롬프트 요청 정보
     * @return 밴드 프롬프트
     */
    private String generateBandPrompt(PromptRequest input) {
        // 1. DB에서 밴드 정보 조회
        Optional<BandDb> bandOpt = bandDbRepository.findByBandNameIgnoreCase(input.getTitle());
        
        // 2. DB 정보가 있으면 활용, 없으면 기본값 사용
        String bandName = input.getTitle();
        String bandNameMeaning = bandOpt.map(BandDb::getBandNameMeaning)
                .orElse("emotional and powerful music");  // DB에 없으면 기본값
        String posterColor = bandOpt.map(BandDb::getPosterColor)
                .orElse("deep blue and purple");  // DB에 없으면 기본값
        String bandSymbol = bandOpt.map(BandDb::getBandSymbol)
                .orElse("stage design");  // DB에 없으면 기본값
        
        // 3. 프롬프트 생성 (DB 정보 우선 사용) - 영어로 번역
        return String.format(
                "A moody alternative rock live performance scene by %s,\n" +
                        "featuring %s,\n" +
                        "set during autumn,\n" +
                        "at %s on %s,\n" +
                        "with a stage design inspired by %s,\n" +
                        "including %s lighting, fog machines and backlights,\n" +
                        "without showing any characters or text.\n" +
                        "No captions, no letters, no words, no logos, no watermarks.",
                translateToEnglish(bandName),
                translateToEnglish(bandNameMeaning),  // DB 밴드 의미 또는 기본값
                translateToEnglish(input.getLocation()),
                input.getDate(),
                translateToEnglish(bandSymbol),  // DB 밴드 상징 또는 기본값
                translateToEnglish(posterColor)  // DB 포스터 색상 또는 기본값
        );
    }
}
