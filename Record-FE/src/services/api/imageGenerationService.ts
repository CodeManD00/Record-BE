import { apiClient } from './client';
import { Result } from '../../utils/result';

/**
 * 백엔드 PromptRequest 타입과 일치하도록 설계
 */
export interface ImageGenerationRequest {
  title: string;
  review: string;

  genre?: string;
  location?: string;
  date?: string;
  cast?: string[];

  imageRequest?: string;
  size?: string;
  n?: number;
  basePrompt?: string;
  model?: string; // 이미지 생성 모델 (예: "dall-e-3", "dall-e-2")
}

/**
 * 백엔드 ImageResponse 구조
 */
export interface ImageGenerationResponse {
  prompt: string;
  imageUrl: string;
  error?: string;
}

export const imageGenerationService = {
  /**
   * 실제 이미지 생성 API
   */
  async generateImage(
    request: ImageGenerationRequest
  ): Promise<Result<ImageGenerationResponse>> {

    // DALL-E 3 모델 지정 (기본값)
    const requestWithModel = {
      ...request,
      model: request.model || 'dall-e-3',
    };

    console.log('🖼 이미지 생성 요청:', JSON.stringify(requestWithModel, null, 2));
    console.log('🖼 요청 필드 확인:', {
      title: requestWithModel.title,
      reviewLength: requestWithModel.review?.length || 0,
      genre: requestWithModel.genre,
      location: requestWithModel.location,
      date: requestWithModel.date,
      basePromptLength: requestWithModel.basePrompt?.length || 0,
      model: requestWithModel.model,
    });

    return apiClient.post<ImageGenerationResponse>('/generate-image', requestWithModel, {
      timeoutMs: 60000,
    });
  },

  /**
   * 파일 포함 버전 (문서 상 존재)
   * POST /generate-image/with-file
   */
  async generateImageWithFile(
    request: ImageGenerationRequest,
    file: { uri: string; type: string; name: string }
  ): Promise<Result<ImageGenerationResponse>> {

    const formData = new FormData();
    formData.append('request', JSON.stringify(request));
    formData.append('file', file as any);

    console.log("🖼 파일 포함 이미지 생성:", request, file);

    return apiClient.postForm<ImageGenerationResponse>(
      '/generate-image/with-file',
      formData,
      { timeoutMs: 60000 }
    );
  },
};
