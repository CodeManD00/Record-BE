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

        // 4. 프롬프트 생성 (DB 정보 우선 사용)
        return String.format("""
                A %s musical theater scene about %s,
                set in %s and depicting %s,
                featuring %s,
                with %s,
                under %s.
                No captions, no letters, no words, no logos, no watermarks.
                """,
                data.get("emotion"),
                musicalSummary,  // DB 요약 또는 후기 분석 결과
                musicalBackground,  // DB 배경 또는 후기 분석 결과
                data.get("relationship"),
                characterPart,
                data.get("actions"),
                data.get("lighting")
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
        
        // 3. 프롬프트 생성 (DB 정보 우선 사용)
        return String.format(
                "A moody alternative rock live performance scene by %s,\n" +
                        "featuring %s,\n" +
                        "set during autumn,\n" +
                        "at %s on %s,\n" +
                        "with a stage design inspired by %s,\n" +
                        "including %s lighting, fog machines and backlights,\n" +
                        "without showing any characters or text.\n" +
                        "No captions, no letters, no words, no logos, no watermarks.",
                bandName,
                bandNameMeaning,  // DB 밴드 의미 또는 기본값
                input.getLocation(),
                input.getDate(),
                bandSymbol,  // DB 밴드 상징 또는 기본값
                posterColor  // DB 포스터 색상 또는 기본값
        );
    }
}
