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
     * 캐릭터 설명에서 JSON 형식을 제거하고 자연어로 변환하는 헬퍼 함수
     * 
     * 변경 사항:
     * - GPT가 반환한 JSON 형식 (예: "{name=Hae, description=...}")을 자연어로 변환
     * - 이유: DALL-E 프롬프트에는 자연어만 들어가야 하므로 JSON 형식은 제거해야 함
     * 
     * @param description 캐릭터 설명 (JSON 형식 포함 가능)
     * @return 정리된 자연어 설명
     */
    private String cleanCharacterDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return "";
        }
        
        // JSON 형식 제거: {name=..., description=...} -> name의 description만 추출
        description = description.trim();
        
        // {name=XXX, description=YYY} 형식 처리
        if (description.startsWith("{") && description.contains("name=")) {
            // name= 뒤의 값을 추출
            int nameStart = description.indexOf("name=");
            if (nameStart >= 0) {
                int nameEnd = description.indexOf(",", nameStart);
                if (nameEnd == -1) nameEnd = description.indexOf("}", nameStart);
                if (nameEnd > nameStart) {
                    String name = description.substring(nameStart + 5, nameEnd).trim();
                    // description= 뒤의 값도 추출
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
        
        // 일반 문자열이면 그대로 반환 (null 체크 후)
        return description;
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
        
        // 나이/성별 번역 (DB 캐릭터 정보용)
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
        
        // 직업/역할 번역 (DB 캐릭터 정보용)
        korean = korean.replace("시인", "poet")
                      .replace("건축가", "architect")
                      .replace("기생", "gisaeng")
                      .replace("배우", "actor")
                      .replace("가수", "singer")
                      .replace("댄서", "dancer");
        
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
     * - 뮤지컬 제목, 요약, 배경 정보 활용
     * - musical_characters 테이블에서 캐릭터의 외형적 특징(나이, 성별, 직업)과 특성(설명) 활용
     * - DB에 정보가 있으면 DB 정보 + 사용자 후기를 모두 반영
     * - DB에 정보가 없으면 후기 분석 결과만 사용
     * 
     * @param input 프롬프트 요청 정보
     * @return 뮤지컬 프롬프트
     */
    private String generateMusicalPrompt(PromptRequest input) {
        // 1단계: musical_db에서 제목으로 조회
        // 제목 정규화: 모든 종류의 공백 제거 (앞뒤 공백, 중간 공백, 특수 공백 문자 등)
        String normalizedTitle = "";
        if (input.getTitle() != null) {
            normalizedTitle = input.getTitle()
                    .trim()                           // 앞뒤 공백 제거
                    .replaceAll("\\s+", "")          // 모든 공백 문자 완전 제거 (한글은 공백 없이도 의미 전달 가능)
                    .replaceAll("[\\u00A0\\u2000-\\u200B\\u2028\\u2029\\uFEFF]", ""); // 특수 공백 문자 제거
        }
        
        // 1-1. 정규화된 제목으로 musical_db 조회
        Optional<MusicalDb> musicalOpt = musicalDbRepository.findByTitle(normalizedTitle);
        
        // 1-2. 정규화된 제목으로 조회 실패 시, 원본 제목으로 시도
        if (!musicalOpt.isPresent() && input.getTitle() != null) {
            String originalTitle = input.getTitle().trim();
            musicalOpt = musicalDbRepository.findByTitle(originalTitle);
            if (musicalOpt.isPresent()) {
                System.out.println("🔍 [DEBUG] 원본 제목으로 musical_db 조회 성공: '" + originalTitle + "'");
            }
        }
        
        // 1-3. 정확 일치 실패 시 부분 일치 시도 (정규화된 제목)
        if (!musicalOpt.isPresent()) {
            List<MusicalDb> musicals = musicalDbRepository.findByTitleContaining(normalizedTitle);
            if (!musicals.isEmpty()) {
                musicalOpt = Optional.of(musicals.get(0));
                System.out.println("🔍 [DEBUG] 부분 일치로 musical_db 조회 성공: '" + musicalOpt.get().getTitle() + "'");
            }
        }
        
        // 1-4. 부분 일치도 실패 시 원본 제목으로 부분 일치 시도
        if (!musicalOpt.isPresent() && input.getTitle() != null) {
            String originalTitle = input.getTitle().trim();
            List<MusicalDb> musicals = musicalDbRepository.findByTitleContaining(originalTitle);
            if (!musicals.isEmpty()) {
                musicalOpt = Optional.of(musicals.get(0));
                System.out.println("🔍 [DEBUG] 원본 제목으로 부분 일치 musical_db 조회 성공: '" + musicalOpt.get().getTitle() + "'");
            }
        }
        
        // 디버깅: 1단계 결과 확인
        System.out.println("🔍 [DEBUG] 제목으로 조회: 원본='" + input.getTitle() + "', 정규화='" + normalizedTitle + "'");
        System.out.println("🔍 [DEBUG] musical_db 조회 결과 존재 여부: " + musicalOpt.isPresent());
        
        // 2단계: musical_db 조회 성공 시, id를 확인하고 musical_characters 조회
        Optional<MusicalDb> musicalWithCharactersOpt = Optional.empty();
        if (musicalOpt.isPresent()) {
            MusicalDb musical = musicalOpt.get();
            Long musicalId = musical.getId();
            System.out.println("🔍 [DEBUG] musical_db에서 찾은 id: " + musicalId);
            System.out.println("🔍 [DEBUG] musical_db에서 찾은 제목: '" + musical.getTitle() + "'");
            
            // 3단계: musical_id로 musical_characters 테이블에서 캐릭터 조회
            musicalWithCharactersOpt = musicalDbRepository.findByIdWithCharacters(musicalId);
            if (musicalWithCharactersOpt.isPresent()) {
                MusicalDb musicalWithCharacters = musicalWithCharactersOpt.get();
                List<MusicalCharacter> characters = musicalWithCharacters.getCharacters();
                System.out.println("🔍 [DEBUG] musical_characters 조회 성공 - 캐릭터 수: " + (characters != null ? characters.size() : 0));
                System.out.println("🔍 [DEBUG] main_character_count: " + musicalWithCharacters.getMainCharacterCount());
            } else {
                System.out.println("🔍 [DEBUG] musical_characters 조회 실패 (뮤지컬은 있지만 캐릭터 없음)");
            }
        }
        
        // 2. 사용자 후기 분석 (항상 수행)
        Map<String, Object> data = reviewAnalysisService.analyzeReview(input.getReview());
        
        // 3. DB에 뮤지컬 정보가 있는 경우와 없는 경우를 분리 처리
        // 변경: 캐릭터 리스트가 비어있어도 main_character_count가 있으면 사용하도록 변경
        if (musicalWithCharactersOpt.isPresent()) {
            MusicalDb musical = musicalWithCharactersOpt.get();
            List<MusicalCharacter> characters = musical.getCharacters();
            
            System.out.println("🔍 [DEBUG] DB 뮤지컬 조회 성공: " + musical.getTitle());
            System.out.println("🔍 [DEBUG] main_character_count: " + musical.getMainCharacterCount());
            System.out.println("🔍 [DEBUG] 실제 캐릭터 수: " + (characters != null ? characters.size() : 0));
            
            // 캐릭터가 있거나 main_character_count가 있으면 DB 정보 사용
            if ((characters != null && !characters.isEmpty()) || musical.getMainCharacterCount() != null) {
            
            // 3-1. DB에서 가져온 뮤지컬 기본 정보
            String musicalSummary = musical.getSummary() != null ? musical.getSummary() : (String) data.get("theme");
            String musicalBackground = musical.getBackground() != null ? musical.getBackground() : (String) data.get("setting");
            
            // 3-1-1. 캐릭터 수 정보 가져오기 (main_character_count 반영)
            // DB에 캐릭터 수 정보가 있으면 사용하고, 없으면 실제 캐릭터 리스트 크기 사용
            Integer characterCount = musical.getMainCharacterCount() != null 
                    ? musical.getMainCharacterCount() 
                    : (characters != null && !characters.isEmpty() ? characters.size() : 3); // 기본값 3
            
            System.out.println("🔍 [DEBUG] 사용할 캐릭터 수: " + characterCount);
            
            // 3-2. 캐릭터 상세 정보 구성 (외형적 특징과 특성 모두 포함)
            // 예시: "Hae (a mid-20s male poet and architect, Yi Sang's true self who lost his memory)"
            // 캐릭터 수만큼만 포함 (main_character_count 반영)
            StringBuilder characterDetails = new StringBuilder();
            
            // 캐릭터 정보가 있으면 사용, 없으면 generic description 사용
            if (characters != null && !characters.isEmpty()) {
                int maxCharacters = Math.min(characters.size(), Math.min(characterCount, 5)); // 최대 5명까지만
                for (int i = 0; i < maxCharacters; i++) {
                    MusicalCharacter character = characters.get(i);
                    if (i > 0) characterDetails.append(", ");
                    
                    // 캐릭터 기본 정보 구성
                    String charInfo = character.getName();
                    
                    // 나이, 성별, 직업 정보 추가
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
                    
                    // 캐릭터 설명 추가
                    if (character.getDescription() != null && !character.getDescription().trim().isEmpty()) {
                        if (charAttributes.length() > 0) charAttributes.append(", ");
                        charAttributes.append(translateToEnglish(character.getDescription()));
                    }
                    
                    // 최종 캐릭터 정보 구성: "이름 (속성들)"
                    if (charAttributes.length() > 0) {
                        charInfo += " (a " + charAttributes.toString() + ")";
                    }
                    
                    characterDetails.append(charInfo);
                }
            } else {
                // 캐릭터 정보가 없으면 generic description 사용
                characterDetails.append(characterCount).append(" distinct characters");
            }
            
            System.out.println("🔍 [DEBUG] 캐릭터 상세 정보: " + characterDetails.toString());
            
            // 3-3. 프롬프트 생성 (DB 정보 + 캐릭터 상세 정보 + 사용자 후기 모두 반영)
            // ✅ main_character_count를 명시적으로 프롬프트에 반영
            // 예: "featuring exactly 3 characters: ..." (3인극인 경우)
            // 더 강력한 제약 조건 추가: "exactly X characters only", "no other characters"
            String prompt = String.format(
                    "A %s musical theater scene about %s,\n" +
                    "set in %s and depicting %s,\n" +
                    "featuring exactly %d characters only: %s.\n" +
                    "CRITICAL: The scene must contain exactly %d characters, no more, no less. No background characters, no extras, no additional people.\n" +
                    "with %s,\n" +
                    "under %s.\n" +
                    "STRICT: Absolutely no text, no letters, no words, no captions, no logos, no watermarks, no writing of any kind visible in the image.",
                    translateToEnglish((String) data.get("emotion")),  // 사용자 후기에서 추출한 감정
                    translateToEnglish(musicalSummary),  // DB 요약 (뮤지컬 사전 정보)
                    translateToEnglish(musicalBackground),  // DB 배경 (뮤지컬 사전 정보)
                    translateToEnglish((String) data.get("relationship")),  // 사용자 후기에서 추출한 관계
                    characterCount,  // ✅ DB에서 가져온 캐릭터 수 (main_character_count)
                    translateToEnglish(characterDetails.toString()),  // DB 캐릭터 상세 정보 (외형적 특징 + 특성)
                    characterCount,  // ✅ 캐릭터 수를 다시 한 번 강조
                    translateToEnglish((String) data.get("actions")),  // 사용자 후기에서 추출한 액션
                    translateToEnglish((String) data.get("lighting"))  // 사용자 후기에서 추출한 조명
            );
            
            System.out.println("🔍 [DEBUG] 생성된 프롬프트:");
            System.out.println(prompt);
            
            return prompt;
            } else {
                // 캐릭터 정보가 없지만 뮤지컬이 DB에 있는 경우
                System.out.println("🔍 [DEBUG] 뮤지컬은 DB에 있지만 캐릭터 정보가 없음");
            }
        }
        
        // ✅ DB에 뮤지컬이 없거나 캐릭터 정보가 없는 경우: 사용자 후기만 참고하여 이미지 생성
        System.out.println("🔍 [DEBUG] DB에 뮤지컬 정보 없음 - 후기 분석만 사용");
        String musicalSummary = (String) data.get("theme");
        String musicalBackground = (String) data.get("setting");
        
        // 캐릭터 정보 구성 (후기 분석 결과만 사용)
        // JSON 형식이 포함되어 있을 수 있으므로 정리
        StringBuilder characterPart = new StringBuilder();
        Object char1 = data.get("character1");
        Object char2 = data.get("character2");
        
        // JSON 형식 제거 및 자연어로 변환
        String cleanChar1 = cleanCharacterDescription(char1 != null ? char1.toString() : "");
        String cleanChar2 = cleanCharacterDescription(char2 != null ? char2.toString() : "");
        
        if (!cleanChar1.isEmpty() && !cleanChar2.isEmpty()) {
            characterPart.append(cleanChar1).append(" and ").append(cleanChar2);
        } else if (!cleanChar1.isEmpty()) {
            characterPart.append(cleanChar1);
        } else if (!cleanChar2.isEmpty()) {
            characterPart.append(cleanChar2);
        }
        
        // 추가 캐릭터 (character3, character4, character5)
        for (int i = 3; i <= 5; i++) {
            String key = "character" + i;
            if (data.containsKey(key)) {
                Object charObj = data.get(key);
                String cleanChar = cleanCharacterDescription(charObj != null ? charObj.toString() : "");
                if (!cleanChar.isEmpty()) {
                    if (characterPart.length() > 0) {
                        characterPart.append(", and ");
                    }
                    characterPart.append(cleanChar);
                }
            }
        }
        
        // 캐릭터 정보가 없으면 기본값 사용
        if (characterPart.length() == 0) {
            characterPart.append("the main characters");
        }
        
        // 프롬프트 생성 (후기 분석 결과만 사용)
        return String.format(
                "A %s musical theater scene about %s,\n" +
                "set in %s and depicting %s,\n" +
                "featuring %s,\n" +
                "with %s,\n" +
                "under %s.\n" +
                "STRICT: Absolutely no text, no letters, no words, no captions, no logos, no watermarks, no writing of any kind visible in the image.",
                translateToEnglish((String) data.get("emotion")),
                translateToEnglish(musicalSummary != null ? musicalSummary : ""),
                translateToEnglish(musicalBackground != null ? musicalBackground : ""),
                translateToEnglish((String) data.get("relationship") != null ? (String) data.get("relationship") : ""),
                translateToEnglish(characterPart.toString()),
                translateToEnglish((String) data.get("actions") != null ? (String) data.get("actions") : ""),
                translateToEnglish((String) data.get("lighting") != null ? (String) data.get("lighting") : "")
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
