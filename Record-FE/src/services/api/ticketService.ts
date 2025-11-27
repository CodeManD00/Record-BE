/**
 * 티켓 = 리뷰 기반 서비스 (백엔드 명세 100% 일치)
 * 단, 요청한 기능: 티켓 상세 조회 / 티켓 이미지 업로드는 기존대로 유지
 */

import { apiClient } from './client';
import { Result } from '../../utils/result';

class TicketService {

  /**
   * 1) 내 티켓 목록 조회
   * GET /api/tickets/user/{userId}
   * 백엔드 응답: 배열로 직접 반환
   */
  async getMyTickets(
    userId: string,
    page: number = 0,
    size: number = 20
  ): Promise<Result<any[]>> {
    // 백엔드가 배열로 직접 반환하므로 타입을 any[]로 변경
    return apiClient.get<any[]>(`/api/tickets/user/${userId}`);
  }

  /**
   * 2) 티켓 생성
   * POST /api/tickets
   * 백엔드 요청 형식:
   * {
   *   userId, performanceTitle, venue, seat, artist, posterUrl, genre, viewDate,
   *   imageUrl, imagePrompt, reviewText, isPublic
   * }
   */
  async createTicket(data: {
    userId: string;
    performanceTitle: string;
    venue?: string;
    seat?: string;
    artist?: string;
    posterUrl?: string | null;
    genre: string;
    viewDate: string; // "YYYY-MM-DD" 형식
    imageUrl?: string | null;
    imagePrompt?: string | null;
    reviewText?: string | null;
    isPublic: boolean;
  }): Promise<Result<any>> {
    return apiClient.post('/api/tickets', data);
  }

  /**
   * 3) 티켓 수정
   * PATCH /api/tickets/{ticketId}
   * Header: X-User-Id, Content-Type: application/json
   * ticketId는 integer (int64) 타입이어야 함
   */
  async updateTicket(
    ticketId: string | number,
    userId: string,
    data: {
      performanceTitle?: string;
      venue?: string;
      seat?: string;
      artist?: string;
      posterUrl?: string | null;
      genre?: string;
      viewDate?: string; // "YYYY-MM-DD" 형식
      imageUrl?: string | null;
      imagePrompt?: string | null;
      reviewText?: string | null;
      isPublic?: boolean;
    }
  ): Promise<Result<any>> {
    console.log('✏️ ticketService.updateTicket 호출됨');
    console.log('✏️ 티켓 ID (원본):', ticketId, '타입:', typeof ticketId);
    
    // ticketId를 숫자로 변환 (백엔드는 integer를 기대함)
    const numericTicketId = typeof ticketId === 'string' ? parseInt(ticketId, 10) : ticketId;
    
    if (isNaN(numericTicketId)) {
      console.error('❌ 티켓 ID를 숫자로 변환할 수 없음:', ticketId);
      return {
        success: false,
        error: {
          code: 'INVALID_TICKET_ID',
          message: '티켓 ID가 유효하지 않습니다.',
        },
      };
    }
    
    console.log('✏️ 티켓 ID (변환 후):', numericTicketId);
    console.log('✏️ 사용자 ID:', userId);
    console.log('✏️ 요청 URL:', `/api/tickets/${numericTicketId}`);
    console.log('✏️ 요청 데이터:', JSON.stringify(data, null, 2));
    console.log('✏️ 요청 헤더:', { 'X-User-Id': userId });
    
    const result = await apiClient.patch(`/api/tickets/${numericTicketId}`, data, {
      headers: { 'X-User-Id': userId },
    });
    
    console.log('✏️ ticketService.updateTicket 결과:', result);
    return result;
  }

  /**
   * 4) 티켓 삭제
   * DELETE /api/tickets/{ticketId}
   * Header: X-User-Id
   * ticketId는 integer (int64) 타입이어야 함
   */
  async deleteTicket(
    ticketId: string | number,
    userId: string
  ): Promise<Result<any>> {
    console.log('🗑️ ticketService.deleteTicket 호출됨');
    console.log('🗑️ 티켓 ID (원본):', ticketId, '타입:', typeof ticketId);
    
    // ticketId를 숫자로 변환 (백엔드는 integer를 기대함)
    const numericTicketId = typeof ticketId === 'string' ? parseInt(ticketId, 10) : ticketId;
    
    if (isNaN(numericTicketId)) {
      console.error('❌ 티켓 ID를 숫자로 변환할 수 없음:', ticketId);
      return {
        success: false,
        error: {
          code: 'INVALID_TICKET_ID',
          message: '티켓 ID가 유효하지 않습니다.',
        },
      };
    }
    
    console.log('🗑️ 티켓 ID (변환 후):', numericTicketId);
    console.log('🗑️ 사용자 ID:', userId);
    console.log('🗑️ 요청 URL:', `/api/tickets/${numericTicketId}`);
    console.log('🗑️ 요청 헤더:', { 'X-User-Id': userId });
    
    const result = await apiClient.delete(`/api/tickets/${numericTicketId}`, undefined, {
      headers: { 'X-User-Id': userId },
    });
    
    console.log('🗑️ ticketService.deleteTicket 결과:', result);
    return result;
  }

  /**
   * 5) 친구 티켓 목록 조회
   * GET /api/tickets/user/{friendId}
   * Header: X-User-Id (현재 사용자 ID, 좋아요 상태 확인용)
   */
  async getFriendTickets(
    friendId: string,
    currentUserId?: string,
    page: number = 0,
    size: number = 20
  ): Promise<Result<any[]>> {
    const headers = currentUserId ? { 'X-User-Id': currentUserId } : undefined;
    return apiClient.get<any[]>(`/api/tickets/user/${friendId}`, headers);
  }

  /**
   * ⚠️ 6) 티켓 상세 조회 (원래 기능 그대로 유지)
   *    현재 백엔드 명세에는 단일 리뷰 조회 API가 없음.
   *    따라서 프론트에서 필요하다면 list에서 찾거나
   *    /api/reviews/{id} API를 백엔드에 추가해야 정확함.
   */
  async getTicket(ticketId: string): Promise<Result<any>> {
    return apiClient.get<any>(`/tickets/${ticketId}`);
  }

  /**
   * ⚠️ 7) 티켓 이미지 업로드 (원래 기능 그대로 유지)
   */
  async uploadTicketImages(
    ticketId: string,
    imageUris: string[]
  ): Promise<Result<{ imageUrls: string[] }>> {

    const formData = new FormData();

    imageUris.forEach((imageUri, index) => {
      formData.append('images', {
        uri: imageUri,
        type: 'image/jpeg',
        name: `ticket_image_${index}.jpg`,
      } as any);
    });

    // multipart는 postForm 사용해야 함
    return apiClient.postForm<{ imageUrls: string[] }>(
      `/tickets/${ticketId}/images`,
      formData
    );
  }

  /**
   * 8) 고급 검색
   * POST /api/tickets/user/{userId}/search
   */
  async searchTickets(
    userId: string,
    searchParams: {
      startDate?: string;
      endDate?: string;
      genre?: string;
      venue?: string;
      artist?: string;
      performanceTitle?: string;
      sortBy?: string;
      sortDirection?: string;
    }
  ): Promise<Result<any[]>> {
    return apiClient.post<any[]>(`/api/tickets/user/${userId}/search`, searchParams);
  }

  /**
   * 9) 티켓 통계 분석
   * GET /api/tickets/user/{userId}/statistics?year=2024
   */
  async getTicketStatistics(
    userId: string,
    year?: number
  ): Promise<Result<any>> {
    const params = year ? `?year=${year}` : '';
    return apiClient.get<any>(`/api/tickets/user/${userId}/statistics${params}`);
  }

  /**
   * 10) 연말 결산
   * GET /api/tickets/user/{userId}/year-in-review?year=2024
   */
  async getYearInReview(
    userId: string,
    year?: number
  ): Promise<Result<any>> {
    const params = year ? `?year=${year}` : '';
    return apiClient.get<any>(`/api/tickets/user/${userId}/year-in-review${params}`);
  }

  /**
   * 11) 좋아요 추가/취소 (토글)
   * POST /api/tickets/{ticketId}/like
   * Header: X-User-Id
   */
  async toggleLike(
    ticketId: string | number,
    userId: string
  ): Promise<Result<{ isLiked: boolean; likeCount: number }>> {
    const numericTicketId = typeof ticketId === 'string' ? parseInt(ticketId, 10) : ticketId;
    if (isNaN(numericTicketId)) {
      return {
        success: false,
        error: {
          code: 'INVALID_TICKET_ID',
          message: '티켓 ID가 유효하지 않습니다.',
        },
      };
    }
    return apiClient.post<{ isLiked: boolean; likeCount: number }>(
      `/api/tickets/${numericTicketId}/like`,
      {},
      {
        headers: { 'X-User-Id': userId },
      }
    );
  }

  /**
   * 12) 좋아요한 사용자 목록 조회 (티켓 소유자만)
   * GET /api/tickets/{ticketId}/likes
   * Header: X-User-Id
   */
  async getLikedUsers(
    ticketId: string | number,
    userId: string
  ): Promise<Result<{ likedUserIds: string[] }>> {
    const numericTicketId = typeof ticketId === 'string' ? parseInt(ticketId, 10) : ticketId;
    if (isNaN(numericTicketId)) {
      return {
        success: false,
        error: {
          code: 'INVALID_TICKET_ID',
          message: '티켓 ID가 유효하지 않습니다.',
        },
      };
    }
    return apiClient.get<{ likedUserIds: string[] }>(
      `/api/tickets/${numericTicketId}/likes`,
      {
        headers: { 'X-User-Id': userId },
      }
    );
  }

}

export const ticketService = new TicketService();
export default ticketService;
