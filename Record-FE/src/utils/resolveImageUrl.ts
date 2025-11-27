// src/utils/resolveImageUrl.ts
// 배포 시 BASE_URL을 EC2 도메인으로 변경해야 함

import { API_BASE_URL } from '../services/api/client';

export const resolveImageUrl = (url?: string | null): string | null => {
  if (!url) return null;

  // 갤러리에서 선택한 로컬 파일은 그대로 사용해야 함
  if (url.startsWith('file://')) return url;

  // 이미 전체 URL인 경우 그대로 사용
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return `${url}?t=${Date.now()}`;
  }

  // API 클라이언트와 동일한 BASE_URL 사용 (동적 import로 최신 값 사용)
  const BASE_URL = API_BASE_URL;

  // 상대 경로인 경우 BASE_URL 추가
  const finalUrl = url.startsWith('/') ? `${BASE_URL}${url}` : `${BASE_URL}/${url}`;

  console.log('🖼 이미지 URL 변환:', { 원본: url, 변환: finalUrl, BASE_URL });

  return `${finalUrl}?t=${Date.now()}`;
};
