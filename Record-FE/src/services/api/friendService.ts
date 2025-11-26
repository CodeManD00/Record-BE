/**
 * 친구 관련 API 서비스 (백엔드 명세와 100% 맞춘 버전)
 */

import { apiClient } from './client';
import { Result } from '../../utils/result';
import {
  Friend,
  FriendRequest,
  CreateFriendRequestData,
  RespondToFriendRequestData,
} from '../../types/friend';
import { userProfileAtom } from '../../atoms/userAtomsApi';
import { getDefaultStore } from 'jotai';
import { resolveImageUrl } from '../../utils/resolveImageUrl';

const store = getDefaultStore();

class FriendService {

  /**
   * 현재 사용자 ID 가져오기
   */
  private getCurrentUserId(): string | null {
    const profile = store.get(userProfileAtom);
    return profile?.id || null;
  }

  /**
   * 친구 목록 조회
   * GET /friendships/{userId}/friends
   * OpenAPI: userId는 현재 사용자 ID (프로필에서 가져옴)
   */
  async getFriends(userId?: string): Promise<Result<{ friends: Friend[] }>> {
    const targetUserId = userId || this.getCurrentUserId();
    if (!targetUserId) {
      return {
        success: false,
        error: { code: 'USER_NOT_FOUND', message: '사용자 ID를 가져올 수 없습니다.' },
      };
    }

    if (__DEV__) {
      console.log('📥 친구 목록 조회:', {
        userId: targetUserId,
        url: `/friendships/${targetUserId}/friends`,
      });
    }

    const result = await apiClient.get(`/friendships/${targetUserId}/friends`);

    if (__DEV__) {
      console.log('📥 친구 목록 응답:', {
        success: result.success,
        data: result.data,
        dataType: Array.isArray(result.data) ? 'array' : typeof result.data,
      });
    }

    // 응답 형식 처리 (배열이거나 { friends: [] } 형태)
    if (result.success && result.data) {
      let friends: any[] = [];

      if (Array.isArray(result.data)) {
        // 배열 형태로 직접 반환 (백엔드 실제 응답 형식)
        friends = result.data;
      } else if (result.data.friends && Array.isArray(result.data.friends)) {
        // { friends: [] } 형태
        friends = result.data.friends;
      } else if (result.data.data && Array.isArray(result.data.data)) {
        // ApiResponseObject로 감싸진 형태
        friends = result.data.data;
      }

      // 백엔드 응답을 Friend 형식으로 변환
      // 백엔드 응답 구조: { id (friendshipId), userId (친구의 ID), userNickname, userProfileImage, friendId (현재 사용자), ... }
      // 친구 정보는 userId, userNickname, userProfileImage에 있음
      const formattedFriends: Friend[] = friends.map((item: any) => {
        // id는 friendshipId (삭제 시 필요)
        // userId가 친구의 ID, userNickname이 친구의 닉네임, userProfileImage가 친구의 프로필 이미지
        const profileImage = item.userProfileImage || item.profileImage || item.avatar;
        return {
          id: String(item.userId || item.id || ''),
          user_id: String(item.userId || ''),
          nickname: item.userNickname || item.nickname || 'Unknown',
          profileImage: profileImage ? resolveImageUrl(profileImage) || undefined : undefined,
          friendshipId: typeof item.id === 'number' ? item.id : (typeof item.id === 'string' ? parseInt(item.id, 10) : undefined),
          createdAt: item.createdAt ? new Date(item.createdAt) : new Date(),
          updatedAt: item.updatedAt ? new Date(item.updatedAt) : new Date(),
        };
      });

      if (__DEV__) {
        console.log('✅ 변환된 친구 목록:', formattedFriends);
      }

      return {
        success: true,
        data: { friends: formattedFriends },
      };
    }

    return result as Result<{ friends: Friend[] }>;
  }

  /**
   * 친구 검색
   * GET /users/search/{userId}
   * OpenAPI: userId는 검색할 사용자 ID (정확한 사용자 ID로 검색)
   * X-User-Id 헤더는 optional
   */
  async searchFriends(params: { query: string; limit?: number }): Promise<Result<{ users: Friend[] }>> {
    const { query } = params;
    if (!query || !query.trim()) {
      return {
        success: false,
        error: {
          code: 'INVALID_QUERY',
          message: '검색어를 입력해주세요.',
        },
      };
    }

    // query를 userId로 사용 (사용자 ID로 검색)
    // OpenAPI 스펙: GET /users/search/{userId}
    // 특정 userId로 사용자 조회 (정확한 ID로만 검색 가능)
    const searchUserId = query.trim();
    
    // X-User-Id 헤더는 optional이지만, 현재 사용자 ID가 있으면 포함
    const currentUserId = this.getCurrentUserId();
    const headers = currentUserId ? { 'X-User-Id': currentUserId } : undefined;
    
    // 경로에 userId 직접 사용 (Spring Boot path variable)
    // 특수 문자가 포함된 경우를 대비해 인코딩하지 않고 그대로 사용
    // Spring Boot가 path variable로 올바르게 파싱함
    const searchUrl = `/users/search/${searchUserId}`;
    
    if (__DEV__) {
      console.log('🔍 사용자 검색 API 호출:', {
        query,
        searchUserId,
        url: searchUrl,
        fullUrl: `http://localhost:8080${searchUrl}`,
        headers,
        currentUserId,
      });
    }
    
    try {
      const result = await apiClient.get(searchUrl, {
        headers,
      });
      
      if (__DEV__) {
        console.log('🔍 사용자 검색 API 응답:', {
          success: result.success,
          hasData: !!result.data,
          error: result.error,
        });
      }
      
      return this.processSearchResult(result);
    } catch (error) {
      if (__DEV__) {
        console.error('🔍 사용자 검색 API 에러:', error);
      }
      return {
        success: false,
        error: {
          code: 'SEARCH_ERROR',
          message: '사용자 검색 중 오류가 발생했습니다.',
        },
      };
    }
  }

  /**
   * 검색 결과 처리
   */
  private processSearchResult(result: Result<any>): Result<{ users: Friend[] }> {
    if (!result.success || !result.data) {
      return {
        success: false,
        error: result.error || {
          code: 'SEARCH_FAILED',
          message: '사용자를 찾을 수 없습니다.',
        },
      };
    }

    // 응답은 ApiResponseObject로 감싸져 있을 수 있음
    // { success: boolean, data: object, message: string }
    let responseData = result.data;
    
    // ApiResponseObject 구조인 경우 (success, data, message)
    if (responseData && typeof responseData === 'object' && 'data' in responseData) {
      responseData = (responseData as any).data;
    }

    // 사용자 객체 또는 배열로 변환
    const users = Array.isArray(responseData) 
      ? responseData 
      : responseData ? [responseData] : [];
    
    return {
      success: true,
      data: { users },
    };
  }

  /**
   * 친구 수 조회
   * GET /friendships/{userId}/friend-count
   * 응답 형식: { "friendCount": 1 }
   */
  async getFriendCount(userId: string): Promise<Result<{ count: number }>> {
    if (__DEV__) {
      console.log('📊 친구 수 조회:', {
        userId,
        url: `/friendships/${userId}/friend-count`,
      });
    }

    const result = await apiClient.get(`/friendships/${userId}/friend-count`);

    if (__DEV__) {
      console.log('📊 친구 수 응답:', {
        success: result.success,
        data: result.data,
      });
    }

    // 응답 형식 처리: { "friendCount": 1 } 또는 { "count": 1 }
    if (result.success && result.data) {
      const count = result.data.friendCount ?? result.data.count ?? 0;
      
      if (__DEV__) {
        console.log('✅ 친구 수:', count);
      }

      return {
        success: true,
        data: { count },
      };
    }

    return result as Result<{ count: number }>;
  }

  /**
   * 보낸 친구 요청 목록 조회
   * GET /friendships/{userId}/sent-requests
   * OpenAPI: userId는 현재 사용자 ID
   */
  async getSentFriendRequests(userId?: string): Promise<Result<{ requests: FriendRequest[] }>> {
    const targetUserId = userId || this.getCurrentUserId();
    if (!targetUserId) {
      return {
        success: false,
        error: { code: 'USER_NOT_FOUND', message: '사용자 ID를 가져올 수 없습니다.' },
      };
    }

    if (__DEV__) {
      console.log('📤 보낸 친구 요청 목록 조회:', {
        userId: targetUserId,
        url: `/friendships/${targetUserId}/sent-requests`,
      });
    }

    const result = await apiClient.get(`/friendships/${targetUserId}/sent-requests`);

    if (__DEV__) {
      console.log('📥 보낸 친구 요청 목록 응답:', {
        success: result.success,
        data: result.data,
        dataType: Array.isArray(result.data) ? 'array' : typeof result.data,
      });
    }

    // 응답 형식 처리 (배열이거나 { requests: [] } 형태)
    if (result.success && result.data) {
      let requests: any[] = [];

      if (Array.isArray(result.data)) {
        // 배열 형태로 직접 반환 (백엔드 실제 응답 형식)
        requests = result.data;
      } else if (result.data.requests && Array.isArray(result.data.requests)) {
        // { requests: [] } 형태
        requests = result.data.requests;
      } else if (result.data.data && Array.isArray(result.data.data)) {
        // ApiResponseObject로 감싸진 형태
        requests = result.data.data;
      }

      // 백엔드 응답 형식:
      // { id, userId (보낸 사람), friendId (받은 사람), friendNickname, friendProfileImage, status, ... }
      // FriendRequest 형식으로 변환
      const formattedRequests: FriendRequest[] = requests.map((req: any) => ({
        id: String(req.id || req.friendshipId || ''),
        fromUserId: String(req.userId || targetUserId), // 보낸 사람 = userId
        toUserId: String(req.friendId || ''), // 받은 사람 = friendId
        nickname: req.friendNickname || req.nickname || 'Unknown',
        user_id: req.friendId || req.user_id || '',
        profileImage: (req.friendProfileImage || req.profileImage) 
          ? resolveImageUrl(req.friendProfileImage || req.profileImage) || undefined
          : undefined,
        status: (req.status || 'PENDING') as any,
        message: req.message,
        createdAt: req.createdAt ? new Date(req.createdAt) : new Date(),
        updatedAt: req.updatedAt ? new Date(req.updatedAt) : new Date(),
      }));

      if (__DEV__) {
        console.log('✅ 변환된 보낸 친구 요청:', formattedRequests);
      }

      return {
        success: true,
        data: { requests: formattedRequests },
      };
    }

    return result as Result<{ requests: FriendRequest[] }>;
  }

  /**
   * 받은 친구 요청 목록 조회
   * GET /friendships/{userId}/received-requests
   * OpenAPI: userId는 현재 사용자 ID
   */
  async getReceivedFriendRequests(userId?: string): Promise<Result<{ requests: FriendRequest[] }>> {
    const targetUserId = userId || this.getCurrentUserId();
    if (!targetUserId) {
      return {
        success: false,
        error: { code: 'USER_NOT_FOUND', message: '사용자 ID를 가져올 수 없습니다.' },
      };
    }

    if (__DEV__) {
      console.log('📥 받은 친구 요청 목록 조회:', {
        userId: targetUserId,
        url: `/friendships/${targetUserId}/received-requests`,
      });
    }

    const result = await apiClient.get(`/friendships/${targetUserId}/received-requests`);

    if (__DEV__) {
      console.log('📥 받은 친구 요청 목록 응답:', {
        success: result.success,
        data: result.data,
        dataType: Array.isArray(result.data) ? 'array' : typeof result.data,
      });
    }

    // 응답 형식 처리 (배열이거나 { requests: [] } 형태)
    if (result.success && result.data) {
      let requests: any[] = [];

      if (Array.isArray(result.data)) {
        // 배열 형태로 직접 반환 (백엔드 실제 응답 형식)
        requests = result.data;
      } else if (result.data.requests && Array.isArray(result.data.requests)) {
        // { requests: [] } 형태
        requests = result.data.requests;
      } else if (result.data.data && Array.isArray(result.data.data)) {
        // ApiResponseObject로 감싸진 형태
        requests = result.data.data;
      }

      // 받은 친구 요청의 경우 백엔드 응답 형식이 다를 수 있음
      // 보낸 요청과 동일한 형식이라면: userId (보낸 사람), friendId (받은 사람 = 나)
      // FriendRequest 형식으로 변환
      const formattedRequests: FriendRequest[] = requests.map((req: any) => {
        // 백엔드 응답에서 id는 friendshipId (숫자)이므로 그대로 사용
        const friendshipId = req.id || req.friendshipId;
        
        if (__DEV__ && !friendshipId) {
          console.warn('⚠️ 받은 친구 요청에 id/friendshipId가 없습니다:', req);
        }

        return {
          id: String(friendshipId || ''),
          fromUserId: String(req.userId || req.fromUserId || ''), // 보낸 사람 = userId
          toUserId: String(req.friendId || req.toUserId || targetUserId), // 받은 사람 = friendId (나)
          nickname: req.userNickname || req.friendNickname || req.nickname || 'Unknown',
          user_id: req.userId || req.user_id || '',
          profileImage: (req.userProfileImage || req.friendProfileImage || req.profileImage)
            ? resolveImageUrl(req.userProfileImage || req.friendProfileImage || req.profileImage) || undefined
            : undefined,
          status: (req.status || 'PENDING') as any,
          message: req.message,
          createdAt: req.createdAt ? new Date(req.createdAt) : new Date(),
          updatedAt: req.updatedAt ? new Date(req.updatedAt) : new Date(),
        };
      });

      if (__DEV__) {
        console.log('✅ 변환된 받은 친구 요청:', formattedRequests);
      }

      return {
        success: true,
        data: { requests: formattedRequests },
      };
    }

    return result as Result<{ requests: FriendRequest[] }>;
  }

  /**
   * 대기중 요청 카운트
   * GET /friendships/{userId}/pending-count
   */
  async getPendingRequestCount(userId: string): Promise<Result<{ count: number }>> {
    return apiClient.get(`/friendships/${userId}/pending-count`);
  }

  /**
   * 친구 요청 보내기
   * POST /friendships/send
   * OpenAPI: X-User-Id 헤더 필요, requestBody: { targetId: string }
   */
  async sendFriendRequest(data: CreateFriendRequestData): Promise<Result<any>> {
    const currentUserId = this.getCurrentUserId();
    if (!currentUserId) {
      return {
        success: false,
        error: { code: 'USER_NOT_FOUND', message: '사용자 ID를 가져올 수 없습니다.' },
      };
    }

    if (__DEV__) {
      console.log('📤 친구 요청 전송:', {
        currentUserId,
        targetId: data.toUserId,
        nickname: data.nickname,
        user_id: data.user_id,
      });
    }

    const result = await apiClient.post('/friendships/send', {
      targetId: data.toUserId,
    }, {
      headers: { 'X-User-Id': currentUserId },
    });

    if (__DEV__) {
      console.log('📥 친구 요청 응답:', {
        success: result.success,
        data: result.data,
        error: result.error,
      });
    }

    return result;
  }

  /**
   * 친구 요청 응답 (수락/거절)
   * POST /friendships/{friendshipId}/accept 또는 /friendships/{friendshipId}/reject
   * OpenAPI: X-User-Id 헤더 필요, friendshipId는 path parameter
   */
  async respondToFriendRequest(data: RespondToFriendRequestData): Promise<Result<any>> {
    const currentUserId = this.getCurrentUserId();
    if (!currentUserId) {
      return {
        success: false,
        error: { code: 'USER_NOT_FOUND', message: '사용자 ID를 가져올 수 없습니다.' },
      };
    }

    // requestId를 friendshipId로 변환
    // 백엔드 응답에서 id는 이미 friendshipId (숫자)이므로 parseInt로 변환
    const friendshipId = parseInt(data.requestId, 10);
    if (isNaN(friendshipId)) {
      if (__DEV__) {
        console.error('❌ 유효하지 않은 requestId:', data.requestId);
      }
      return {
        success: false,
        error: { code: 'INVALID_REQUEST_ID', message: '유효하지 않은 요청 ID입니다.' },
      };
    }

    if (__DEV__) {
      console.log('📤 친구 요청 응답:', {
        requestId: data.requestId,
        friendshipId,
        accept: data.accept,
        currentUserId,
      });
    }

    const endpoint = data.accept 
      ? `/friendships/${friendshipId}/accept`
      : `/friendships/${friendshipId}/reject`;
    
    const result = await apiClient.post(endpoint, null, {
      headers: { 'X-User-Id': currentUserId },
    });

    if (__DEV__) {
      if (!result.success) {
        console.error('❌ 친구 요청 응답 실패:', {
          endpoint,
          error: result.error,
        });
      }
    }

    return result;
  }

  /**
   * 친구 관계 삭제
   * DELETE /friendships/{friendshipId}
   * OpenAPI: X-User-Id 헤더 필요
   * friendshipId는 숫자여야 함
   */
  async removeFriend(friendshipId: string | number): Promise<Result<any>> {
    const currentUserId = this.getCurrentUserId();
    if (!currentUserId) {
      return {
        success: false,
        error: { code: 'USER_NOT_FOUND', message: '사용자 ID를 가져올 수 없습니다.' },
      };
    }

    // friendshipId를 숫자로 변환
    const numericFriendshipId = typeof friendshipId === 'string' ? parseInt(friendshipId, 10) : friendshipId;
    if (isNaN(numericFriendshipId)) {
      if (__DEV__) {
        console.error('❌ 유효하지 않은 friendshipId:', friendshipId);
      }
      return {
        success: false,
        error: { code: 'INVALID_FRIENDSHIP_ID', message: '유효하지 않은 친구 관계 ID입니다.' },
      };
    }

    if (__DEV__) {
      console.log('🗑️ 친구 삭제:', {
        friendshipId: numericFriendshipId,
        currentUserId,
        url: `/friendships/${numericFriendshipId}`,
      });
    }

    const result = await apiClient.delete(`/friendships/${numericFriendshipId}`, null, {
      headers: { 'X-User-Id': currentUserId },
    });

    if (__DEV__) {
      if (result.success) {
        console.log('✅ 친구 삭제 성공');
      } else {
        console.error('❌ 친구 삭제 실패:', result.error);
      }
    }

    return result;
  }

  /**
   * 친구 요청 취소
   * DELETE /friendships/{friendshipId}/cancel
   */
  async cancelFriendRequest(requestId: string): Promise<Result<any>> {
    const friendshipId = parseInt(requestId, 10);
    if (isNaN(friendshipId)) {
      return {
        success: false,
        error: { code: 'INVALID_REQUEST_ID', message: '유효하지 않은 요청 ID입니다.' },
      };
    }
    return apiClient.delete(`/friendships/${friendshipId}/cancel`, null);
  }
}

export const friendService = new FriendService();
export default friendService;
