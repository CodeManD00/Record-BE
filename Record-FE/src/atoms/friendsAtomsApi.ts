/**
 * 친구 관련 API 연동 atoms
 * 기존 friendsAtoms.ts를 API 연동으로 리팩토링
 */

import { atom } from 'jotai';
import { atomWithStorage } from 'jotai/utils';
import { friendService } from '../services/api/index';
import { Friend, FriendRequest, CreateFriendRequestData, RespondToFriendRequestData } from '../types/friend';
import { userProfileAtom } from './userAtoms';
// Result 타입을 직접 정의 (임시 해결책)
type Result<T> = {
  success: true;
  data: T;
  error?: never;
} | {
  success: false;
  data?: never;
  error: { code: string; message: string; details?: any };
};

class ResultFactory {
  static success<T>(data: T): Result<T> {
    return { success: true, data };
  }
  
  static failure<T>(error: { code: string; message: string; details?: any }): Result<T> {
    return { success: false, error };
  }
}
import { ApiState, createInitialApiState, apiStateHelpers, isCacheValid } from './apiAtoms';

/**
 * 친구 목록 상태
 */
export const friendsStateAtom = atom<ApiState<Friend[]>>(createInitialApiState<Friend[]>());

/**
 * 받은 친구 요청 상태
 */
export const receivedRequestsStateAtom = atom<ApiState<FriendRequest[]>>(createInitialApiState<FriendRequest[]>());

/**
 * 보낸 친구 요청 상태
 */
export const sentRequestsStateAtom = atom<ApiState<FriendRequest[]>>(createInitialApiState<FriendRequest[]>());

/**
 * 친구 검색 결과 상태
 */
export const friendSearchStateAtom = atom<ApiState<Friend[]>>(createInitialApiState<Friend[]>());

/**
 * 친구 수 상태
 */
export const friendCountStateAtom = atom<ApiState<number>>(createInitialApiState<number>());

/**
 * 현재 검색 쿼리
 */
export const searchQueryAtom = atom<string>('');

/**
 * 친구 목록 조회 (캐시 지원)
 */
export const fetchFriendsAtom = atom(
  null,
  async (get, set, force: boolean = false) => {
    const currentState = get(friendsStateAtom);
    
    // 캐시가 유효하고 강제 새로고침이 아닌 경우 스킵
    if (!force && currentState.data && isCacheValid(currentState.lastFetch)) {
      return ResultFactory.success(currentState.data);
    }

    // 로딩 상태 설정
    set(friendsStateAtom, apiStateHelpers.setLoading(currentState));

    try {
      const result = await friendService.getFriends();
      
      if (result.success && result.data) {
        const friends = result.data.friends;
        set(friendsStateAtom, apiStateHelpers.setSuccess(currentState, friends));
        return ResultFactory.success(friends);
      } else {
        const errorMessage = result.error?.message || '친구 목록을 불러오는데 실패했습니다';
        set(friendsStateAtom, apiStateHelpers.setError(currentState, errorMessage));
        return ResultFactory.failure(result.error!);
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다';
      set(friendsStateAtom, apiStateHelpers.setError(currentState, errorMessage));
      return ResultFactory.failure({ message: errorMessage, code: 'UNKNOWN_ERROR' });
    }
  }
);

/**
 * 친구 수 조회 (캐시 지원)
 */
export const fetchFriendCountAtom = atom(
  null,
  async (get, set, userId: string, force: boolean = false) => {
    const currentState = get(friendCountStateAtom);
    
    // 캐시가 유효하고 강제 새로고침이 아닌 경우 스킵
    if (!force && currentState.data !== null && isCacheValid(currentState.lastFetch)) {
      return ResultFactory.success(currentState.data);
    }

    // 로딩 상태 설정
    set(friendCountStateAtom, apiStateHelpers.setLoading(currentState));

    try {
      const result = await friendService.getFriendCount(userId);
      
      if (result.success && result.data) {
        const count = result.data.count;
        set(friendCountStateAtom, apiStateHelpers.setSuccess(currentState, count));
        return ResultFactory.success(count);
      } else {
        const errorMessage = result.error?.message || '친구 수를 불러오는데 실패했습니다';
        set(friendCountStateAtom, apiStateHelpers.setError(currentState, errorMessage));
        return ResultFactory.failure(result.error!);
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다';
      set(friendCountStateAtom, apiStateHelpers.setError(currentState, errorMessage));
      return ResultFactory.failure({ message: errorMessage, code: 'UNKNOWN_ERROR' });
    }
  }
);

/**
 * 친구 검색
 */
export const searchFriendsAtom = atom(
  null,
  async (get, set, query: string) => {
    if (!query.trim()) {
      set(friendSearchStateAtom, apiStateHelpers.reset<Friend[]>());
      return ResultFactory.success([]);
    }

    const currentState = get(friendSearchStateAtom);
    set(friendSearchStateAtom, apiStateHelpers.setLoading(currentState));
    set(searchQueryAtom, query);

    try {
      const result = await friendService.searchFriends({ query, limit: 20 });
      
      if (result.success && result.data) {
        const users = result.data.users;
        set(friendSearchStateAtom, apiStateHelpers.setSuccess(currentState, users));
        return ResultFactory.success(users);
      } else {
        const errorMessage = result.error?.message || '사용자 검색에 실패했습니다';
        set(friendSearchStateAtom, apiStateHelpers.setError(currentState, errorMessage));
        return ResultFactory.failure(result.error!);
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '검색 중 오류가 발생했습니다';
      set(friendSearchStateAtom, apiStateHelpers.setError(currentState, errorMessage));
      return ResultFactory.failure({ message: errorMessage, code: 'SEARCH_ERROR' });
    }
  }
);

/**
 * 받은 친구 요청 조회
 */
export const fetchReceivedRequestsAtom = atom(
  null,
  async (get, set, force: boolean = false) => {
    const currentState = get(receivedRequestsStateAtom);
    
    if (!force && currentState.data && isCacheValid(currentState.lastFetch)) {
      return ResultFactory.success(currentState.data);
    }

    set(receivedRequestsStateAtom, apiStateHelpers.setLoading(currentState));

    try {
      const result = await friendService.getReceivedFriendRequests();
      
      if (result.success && result.data) {
        const requests = result.data.requests;
        set(receivedRequestsStateAtom, apiStateHelpers.setSuccess(currentState, requests));
        return ResultFactory.success(requests);
      } else {
        const errorMessage = result.error?.message || '친구 요청을 불러오는데 실패했습니다';
        set(receivedRequestsStateAtom, apiStateHelpers.setError(currentState, errorMessage));
        return ResultFactory.failure(result.error!);
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다';
      set(receivedRequestsStateAtom, apiStateHelpers.setError(currentState, errorMessage));
      return ResultFactory.failure({ message: errorMessage, code: 'UNKNOWN_ERROR' });
    }
  }
);

/**
 * 보낸 친구 요청 조회
 */
export const fetchSentRequestsAtom = atom(
  null,
  async (get, set, force: boolean = false) => {
    const currentState = get(sentRequestsStateAtom);
    
    if (!force && currentState.data && isCacheValid(currentState.lastFetch)) {
      return ResultFactory.success(currentState.data);
    }

    set(sentRequestsStateAtom, apiStateHelpers.setLoading(currentState));

    try {
      const result = await friendService.getSentFriendRequests();
      
      if (result.success && result.data) {
        const requests = result.data.requests;
        set(sentRequestsStateAtom, apiStateHelpers.setSuccess(currentState, requests));
        return ResultFactory.success(requests);
      } else {
        const errorMessage = result.error?.message || '보낸 요청을 불러오는데 실패했습니다';
        set(sentRequestsStateAtom, apiStateHelpers.setError(currentState, errorMessage));
        return ResultFactory.failure(result.error!);
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다';
      set(sentRequestsStateAtom, apiStateHelpers.setError(currentState, errorMessage));
      return ResultFactory.failure({ message: errorMessage, code: 'UNKNOWN_ERROR' });
    }
  }
);

/**
 * 친구 요청 보내기 (낙관적 업데이트)
 */
export const sendFriendRequestAtom = atom(
  null,
  async (get, set, data: CreateFriendRequestData) => {
    try {
      // 낙관적 업데이트: 보낸 요청 목록에 임시 추가
      const currentSentState = get(sentRequestsStateAtom);
      if (currentSentState.data) {
        const optimisticRequest: FriendRequest = {
          id: `temp_${Date.now()}`,
          fromUserId: 'current_user',
          toUserId: data.toUserId,
          nickname: data.nickname,
          user_id: data.user_id,
          status: 'PENDING' as any,
          createdAt: new Date(),
          updatedAt: new Date(),
        };
        
        const updatedRequests = [...currentSentState.data, optimisticRequest];
        set(sentRequestsStateAtom, apiStateHelpers.setSuccess(currentSentState, updatedRequests));
      }

      // 실제 API 호출
      const result = await friendService.sendFriendRequest(data);
      
      if (result.success) {
        // 성공 시 보낸 요청 목록 새로고침
        set(fetchSentRequestsAtom, true);
        return result;
      } else {
        // 실패 시 낙관적 업데이트 롤백
        set(fetchSentRequestsAtom, true);
        return result;
      }
    } catch (error) {
      // 에러 시 롤백
      set(fetchSentRequestsAtom, true);
      const errorMessage = error instanceof Error ? error.message : '친구 요청 전송에 실패했습니다';
      return ResultFactory.failure({ message: errorMessage, code: 'SEND_REQUEST_ERROR' });
    }
  }
);

/**
 * 친구 요청 응답 (수락/거절)
 */
export const respondToFriendRequestAtom = atom(
  null,
  async (get, set, data: RespondToFriendRequestData) => {
    try {
      // 낙관적 업데이트: 받은 요청 목록에서 제거
      const currentReceivedState = get(receivedRequestsStateAtom);
      if (currentReceivedState.data) {
        const updatedRequests = currentReceivedState.data.filter(req => req.id !== data.requestId);
        set(receivedRequestsStateAtom, apiStateHelpers.setSuccess(currentReceivedState, updatedRequests));
      }

      // 수락하는 경우 친구 목록에 낙관적 추가
      if (data.accept) {
        const request = currentReceivedState.data?.find(req => req.id === data.requestId);
        if (request) {
          const currentFriendsState = get(friendsStateAtom);
          if (currentFriendsState.data) {
            const newFriend: Friend = {
              id: request.fromUserId,
              nickname: request.nickname,
              user_id: request.user_id,
              profileImage: request.profileImage,
              createdAt: new Date(),
              updatedAt: new Date(),
            };
            
            const updatedFriends = [...currentFriendsState.data, newFriend];
            set(friendsStateAtom, apiStateHelpers.setSuccess(currentFriendsState, updatedFriends));
          }
        }
      }

      // 실제 API 호출
      const result = await friendService.respondToFriendRequest(data);
      
      if (result.success) {
        // 성공 시 관련 데이터 새로고침
        await set(fetchReceivedRequestsAtom, true);
        if (data.accept) {
          // 친구 수락 시 친구 목록과 친구 수 강제 새로고침
          await set(fetchFriendsAtom, true);
          // 친구 수도 새로고침 (userId 필요)
          const userProfile = get(userProfileAtom);
          const userId = userProfile?.id;
          if (userId) {
            await set(fetchFriendCountAtom, userId, true);
          }
        }
        return result;
      } else {
        // 실패 시 낙관적 업데이트 롤백
        await set(fetchReceivedRequestsAtom, true);
        await set(fetchFriendsAtom, true);
        return result;
      }
    } catch (error) {
      // 에러 시 롤백
      set(fetchReceivedRequestsAtom, true);
      set(fetchFriendsAtom, true);
      const errorMessage = error instanceof Error ? error.message : '친구 요청 응답에 실패했습니다';
      return ResultFactory.failure({ message: errorMessage, code: 'RESPOND_REQUEST_ERROR' });
    }
  }
);

/**
 * 친구 삭제
 */
export const removeFriendAtom = atom(
  null,
  async (get, set, friend: Friend | string) => {
    try {
      // friend가 객체인 경우 friendshipId 추출, 문자열인 경우 friendshipId로 사용
      let friendshipId: string | number;
      let friendId: string;
      
      if (typeof friend === 'string') {
        // 문자열인 경우 (기존 호환성)
        friendshipId = friend;
        friendId = friend;
      } else {
        // Friend 객체인 경우
        if (!friend.friendshipId) {
          const errorMessage = '친구 관계 ID가 없습니다. 친구 목록을 새로고침해주세요.';
          console.error('❌ friendshipId 없음:', friend);
          return ResultFactory.failure({ message: errorMessage, code: 'FRIENDSHIP_ID_MISSING' });
        }
        friendshipId = friend.friendshipId;
        friendId = friend.id;
      }

      console.log('🗑️ removeFriendAtom 호출:', { friendshipId, friendId, friend });

      // 낙관적 업데이트: 친구 목록에서 제거
      const currentFriendsState = get(friendsStateAtom);
      if (currentFriendsState.data) {
        const updatedFriends = currentFriendsState.data.filter(f => f.id !== friendId);
        set(friendsStateAtom, apiStateHelpers.setSuccess(currentFriendsState, updatedFriends));
        console.log('🗑️ 낙관적 업데이트 완료, 남은 친구 수:', updatedFriends.length);
      }

      // 실제 API 호출
      const result = await friendService.removeFriend(friendshipId);
      
      if (result.success) {
        console.log('✅ 친구 삭제 성공');
        // 성공 시 친구 목록과 친구 수 새로고침
        await set(fetchFriendsAtom, true);
        // 친구 수도 새로고침 (userId 필요)
        const userProfile = get(userProfileAtom);
        const userId = userProfile?.id;
        if (userId) {
          await set(fetchFriendCountAtom, userId, true);
        }
        return result;
      } else {
        console.error('❌ 친구 삭제 실패:', result.error);
        // 실패 시 롤백
        await set(fetchFriendsAtom, true);
        return result;
      }
    } catch (error) {
      // 에러 시 롤백
      await set(fetchFriendsAtom, true);
      const errorMessage = error instanceof Error ? error.message : '친구 삭제에 실패했습니다';
      return ResultFactory.failure({ message: errorMessage, code: 'REMOVE_FRIEND_ERROR' });
    }
  }
);

/**
 * 친구 요청 취소
 */
export const cancelFriendRequestAtom = atom(
  null,
  async (get, set, requestId: string) => {
    try {
      // 낙관적 업데이트: 보낸 요청 목록에서 제거
      const currentSentState = get(sentRequestsStateAtom);
      if (currentSentState.data) {
        const updatedRequests = currentSentState.data.filter(req => req.id !== requestId);
        set(sentRequestsStateAtom, apiStateHelpers.setSuccess(currentSentState, updatedRequests));
      }

      // 실제 API 호출
      const result = await friendService.cancelFriendRequest(requestId);
      
      if (result.success) {
        return result;
      } else {
        // 실패 시 롤백
        set(fetchSentRequestsAtom, true);
        return result;
      }
    } catch (error) {
      // 에러 시 롤백
      set(fetchSentRequestsAtom, true);
      const errorMessage = error instanceof Error ? error.message : '요청 취소에 실패했습니다';
      return ResultFactory.failure({ message: errorMessage, code: 'CANCEL_REQUEST_ERROR' });
    }
  }
);

/**
 * 읽기 전용 atoms (컴포넌트에서 사용)
 */
export const friendsAtom = atom<Friend[]>((get) => {
  const state = get(friendsStateAtom);
  return state.data || [];
});

export const receivedFriendRequestsAtom = atom<FriendRequest[]>((get) => {
  const state = get(receivedRequestsStateAtom);
  return state.data || [];
});

export const sentFriendRequestsAtom = atom<FriendRequest[]>((get) => {
  const state = get(sentRequestsStateAtom);
  return state.data || [];
});

export const friendSearchResultsAtom = atom<Friend[]>((get) => {
  const state = get(friendSearchStateAtom);
  return state.data || [];
});

export const friendCountAtom = atom<number>((get) => {
  const state = get(friendCountStateAtom);
  return state.data ?? 0;
});

/**
 * 로딩 상태 atoms
 */
export const friendsLoadingAtom = atom<boolean>((get) => {
  const state = get(friendsStateAtom);
  return state.loading === 'loading';
});

export const friendSearchLoadingAtom = atom<boolean>((get) => {
  const state = get(friendSearchStateAtom);
  return state.loading === 'loading';
});

export const receivedRequestsLoadingAtom = atom<boolean>((get) => {
  const state = get(receivedRequestsStateAtom);
  return state.loading === 'loading';
});

/**
 * 에러 상태 atoms
 */
export const friendsErrorAtom = atom<string | null>((get) => {
  const state = get(friendsStateAtom);
  return state.error;
});

export const friendSearchErrorAtom = atom<string | null>((get) => {
  const state = get(friendSearchStateAtom);
  return state.error;
});

export const receivedRequestsErrorAtom = atom<string | null>((get) => {
  const state = get(receivedRequestsStateAtom);
  return state.error;
});
