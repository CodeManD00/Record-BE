/**
 * STT (Speech-to-Text) API 서비스 — 백엔드 명세서 100% 일치 완전 수정본
 */

import { apiClient } from './client';
import { Result } from '../../utils/result';

/**
 * 백엔드의 TranscriptionResponse 구조 기반 타입
 */
export interface TranscriptionResponse {
  id: number | null;
  fileName: string;
  createdAt: string;
  transcript?: string;
  resultText?: string;
  summary: string | null;
  finalReview: string | null;
}

/**
 * STT 서비스 (명세 일치)
 */
class SttService {

  /**
   * 1) Whisper STT 변환 + DB 저장
   * POST /stt/transcribe-and-save
   * 
   * 타임아웃 설정 이유:
   * - 서버 측(WhisperService)에서 타임아웃이 120초로 설정되어 있음
   * - STT 변환은 오디오 파일 크기에 따라 처리 시간이 오래 걸릴 수 있음
   * - 기본 타임아웃(20초)으로는 긴 오디오 파일 처리 시 클라이언트가 요청을 중단함
   * - 서버는 정상적으로 처리 완료했지만 클라이언트는 타임아웃 에러를 받게 됨
   * - 따라서 서버 타임아웃(120초)보다 여유있게 130초로 설정하여 안정성 확보
   */
  async transcribeAndSave(
    audioUri: string,
    fileName: string = 'recording.m4a',
    fileType: string = 'audio/m4a',
    userId: string
  ): Promise<Result<TranscriptionResponse>> {

    const formData = new FormData();
    formData.append('file', {
      uri: audioUri,
      type: fileType,
      name: fileName,
    } as any);
    formData.append('userId', userId);

    console.log('🎤 STT transcribe-and-save 요청 → FormData 생성 완료');

    // STT 변환은 시간이 오래 걸릴 수 있으므로 타임아웃을 130초로 설정
    // 서버 측 타임아웃(120초)보다 여유있게 설정하여 안정성 확보
    return apiClient.postForm('/stt/transcribe-and-save', formData, { timeoutMs: 130000 }); // 130초
  }

  /**
   * 2) STT 텍스트 기반 요약
   * POST /review/summarize
   *
   * request body: ReviewRequest {
   *   transcriptionId?: number;
   *   text?: string;
   * }
   */
  async summarizeReview(
    text: string,
    transcriptionId?: number
  ): Promise<Result<TranscriptionResponse>> {

    console.log('📝 후기 요약 요청:', { text, transcriptionId });

    // OpenAI API 호출은 시간이 오래 걸릴 수 있으므로 타임아웃을 60초로 설정
    return apiClient.post(
      '/review/summarize',
      {
        text,
        transcriptionId,
      },
      { timeoutMs: 60000 } // 60초
    );
  }

  /**
   * 3) 후기 조직화 (Organize)
   * POST /review/organize
   *
   * request: ReviewRequest 같은 구조
   */
  async organizeReview(
    text: string,
    transcriptionId?: number
  ): Promise<Result<TranscriptionResponse>> {

    // OpenAI API 호출은 시간이 오래 걸릴 수 있으므로 타임아웃을 60초로 설정
    return apiClient.post(
      '/review/organize',
      {
        text,
        transcriptionId,
      },
      { timeoutMs: 60000 } // 60초
    );
  }

  /**
   * 4) 후기 finalize (최종본 확정)
   * POST /reviews/finalize
   */
  async finalizeReview(
    transcriptionId: number,
    extraNotes?: string
  ): Promise<Result<TranscriptionResponse>> {

    return apiClient.post('/reviews/finalize', {
      transcriptionId,
      extraNotes,
    });
  }
}

export const sttService = new SttService();
export default sttService;
