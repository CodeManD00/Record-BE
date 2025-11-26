// src/utils/resolveImageUrl.ts
// 배포 시 BASE_URL을 EC2 도메인으로 변경해야 함

export const resolveImageUrl = (url?: string | null): string | null => {
  if (!url) return null;

  // 갤러리에서 선택한 로컬 파일은 그대로 사용해야 함
  if (url.startsWith('file://')) return url;

  // API 클라이언트와 동일한 BASE_URL 사용
  const BASE_URL = __DEV__
    ? 'http://localhost:8080'
    : 'https://api.ticketbook.app';

  const finalUrl = url.startsWith('http') ? url : `${BASE_URL}${url}`;

  return `${finalUrl}?t=${Date.now()}`;
};
