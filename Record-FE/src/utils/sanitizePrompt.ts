/**
 * 이미지 생성 프롬프트를 OpenAI의 안전 정책에 맞게 정제하는 유틸리티
 * 
 * OpenAI DALL-E 3의 content policy violation을 방지하기 위해
 * 폭력적이거나 민감한 내용을 더 안전한 표현으로 변환합니다.
 */

/**
 * 문제가 될 수 있는 단어와 그 대체 표현
 */
const SAFE_REPLACEMENTS: Array<{ pattern: RegExp; replacement: string }> = [
  // 폭력 관련
  { pattern: /\bkidnapping\b/gi, replacement: 'bringing together' },
  { pattern: /\bkidnap\b/gi, replacement: 'bring together' },
  { pattern: /\bkidnapped\b/gi, replacement: 'brought together' },
  { pattern: /\bkidnapper\b/gi, replacement: 'character' },
  { pattern: /\bviolence\b/gi, replacement: 'intense emotion' },
  { pattern: /\bviolent\b/gi, replacement: 'intense' },
  { pattern: /\bkilling\b/gi, replacement: 'parting' },
  { pattern: /\bkilled\b/gi, replacement: 'separated' },
  { pattern: /\bmurder\b/gi, replacement: 'separation' },
  { pattern: /\bmurdered\b/gi, replacement: 'separated' },
  { pattern: /\bdeath\b/gi, replacement: 'departure' },
  { pattern: /\bdying\b/gi, replacement: 'fading' },
  { pattern: /\bdead\b/gi, replacement: 'gone' },
  { pattern: /\bweapon\b/gi, replacement: 'object' },
  { pattern: /\bweapons\b/gi, replacement: 'objects' },
  { pattern: /\bgun\b/gi, replacement: 'object' },
  { pattern: /\bguns\b/gi, replacement: 'objects' },
  { pattern: /\bknife\b/gi, replacement: 'object' },
  { pattern: /\bknives\b/gi, replacement: 'objects' },
  
  // 정치/민감한 내용 (컨텍스트를 유지하면서 안전하게)
  { pattern: /\bJapanese colonial period\b/gi, replacement: 'historical period' },
  { pattern: /\bcolonial period\b/gi, replacement: 'historical period' },
  { pattern: /\bcolonization\b/gi, replacement: 'historical era' },
  { pattern: /\bwar\b/gi, replacement: 'conflict' },
  { pattern: /\bwars\b/gi, replacement: 'conflicts' },
  { pattern: /\bwarfare\b/gi, replacement: 'struggle' },
  { pattern: /\bfight\b/gi, replacement: 'confrontation' },
  { pattern: /\bfighting\b/gi, replacement: 'confrontation' },
  { pattern: /\bfought\b/gi, replacement: 'confronted' },
  
  // 기타 민감한 표현
  { pattern: /\bterror\b/gi, replacement: 'fear' },
  { pattern: /\bterrorism\b/gi, replacement: 'tension' },
  { pattern: /\bterrorist\b/gi, replacement: 'figure' },
  { pattern: /\bhate\b/gi, replacement: 'strong dislike' },
  { pattern: /\bhated\b/gi, replacement: 'disliked' },
];

/**
 * 프롬프트를 정제하여 OpenAI 안전 정책에 맞게 변환
 * 
 * @param prompt 원본 프롬프트
 * @returns 정제된 프롬프트
 */
export const sanitizePrompt = (prompt: string | null | undefined): string => {
  if (!prompt || typeof prompt !== 'string') {
    return '';
  }

  let sanitized = prompt.trim();

  // 각 대체 규칙 적용
  for (const { pattern, replacement } of SAFE_REPLACEMENTS) {
    sanitized = sanitized.replace(pattern, replacement);
  }

  // 연속된 공백 정리
  sanitized = sanitized.replace(/\s+/g, ' ');

  // 문장 부호 정리
  sanitized = sanitized.replace(/\s+([.,!?;:])/g, '$1');

  return sanitized.trim();
};

/**
 * 프롬프트가 안전한지 간단히 검사
 * 
 * @param prompt 검사할 프롬프트
 * @returns 안전 여부
 */
export const isPromptSafe = (prompt: string | null | undefined): boolean => {
  if (!prompt) return true;

  const dangerousPatterns = [
    /\bkidnap/i,
    /\bmurder/i,
    /\bkill\b/i,
    /\bdeath\b/i,
    /\bweapon/i,
    /\bgun\b/i,
    /\bknife\b/i,
    /\bterror/i,
  ];

  return !dangerousPatterns.some(pattern => pattern.test(prompt));
};

