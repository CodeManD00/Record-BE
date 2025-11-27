/**
 * 티켓 관련 API 연동 atoms
 * 기존 ticketsAtoms.ts를 API 연동으로 리팩토링
 */

import { atom } from 'jotai';
import { ticketService } from '../services/api/index';
import { Ticket, TicketStatus } from '../types/ticket';
import { userProfileAtom } from './userAtomsApi';
import { resolveImageUrl } from '../utils/resolveImageUrl';
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
 * 내 티켓 목록 상태
 */
export const myTicketsStateAtom = atom<ApiState<Ticket[]>>(createInitialApiState<Ticket[]>());

/**
 * 친구 티켓 맵 상태 (friendId -> tickets)
 */
export const friendTicketsMapStateAtom = atom<ApiState<Map<string, Ticket[]>>>(
  createInitialApiState<Map<string, Ticket[]>>()
);

/**
 * 현재 선택된 티켓 필터
 */
export const ticketFilterAtom = atom<{
  status?: TicketStatus;
  startDate?: string;
  endDate?: string;
}>({});

/**
 * 내 티켓 목록 조회
 */
export const fetchMyTicketsAtom = atom(
  null,
  async (get, set, force: boolean = false) => {
    const currentState = get(myTicketsStateAtom);
    
    if (!force && currentState.data && isCacheValid(currentState.lastFetch)) {
      return ResultFactory.success(currentState.data);
    }

    set(myTicketsStateAtom, apiStateHelpers.setLoading(currentState));

    try {
      // 사용자 ID 가져오기
      const userProfile = get(userProfileAtom);
      const userId = userProfile?.id || userProfile?.user_id;
      
      if (!userId) {
        const errorMessage = '사용자 ID를 가져올 수 없습니다. 다시 로그인해주세요.';
        set(myTicketsStateAtom, apiStateHelpers.setError(currentState, errorMessage));
        return ResultFactory.failure({ message: errorMessage, code: 'USER_NOT_FOUND' });
      }

      const filter = get(ticketFilterAtom);
      const result = await ticketService.getMyTickets(
        userId,
        0, // page
        100 // size (충분히 큰 값)
      );
      
      if (result.success && result.data) {
        // 백엔드 응답 형식: 배열로 직접 반환
        // [{ id, userId, performanceTitle, theater, genre, viewDate, imageUrl, reviewText, ... }]
        const ticketsList = Array.isArray(result.data) ? result.data : [];
        
        // 백엔드 응답을 Ticket 형식으로 변환
        const tickets: Ticket[] = ticketsList.map((ticket: any) => {
          // viewDate를 Date로 변환
          const performedAt = ticket.viewDate ? new Date(ticket.viewDate) : new Date();
          
          // genre를 백엔드 형식에서 프론트엔드 형식으로 변환
          let genre: string | null = null;
          if (ticket.genre) {
            const genreMap: Record<string, string> = {
              'BAND': '밴드',
              'MUSICAL': '연극/뮤지컬',
              'PLAY': '연극/뮤지컬',
            };
            genre = genreMap[ticket.genre] || ticket.genre;
          }
          
          // 이미지 URL 처리 (resolveImageUrl 사용)
          const images: string[] = [];
          if (ticket.imageUrl) {
            const resolvedUrl = resolveImageUrl(ticket.imageUrl);
            if (resolvedUrl) {
              images.push(resolvedUrl);
            }
          }
          if (ticket.posterUrl) {
            const resolvedUrl = resolveImageUrl(ticket.posterUrl);
            if (resolvedUrl) {
              images.push(resolvedUrl);
            }
          }
          
          return {
            id: String(ticket.id || ''),
            user_id: ticket.userId || userId, // Ticket 타입에 필수
            userId: ticket.userId || userId, // 하위 호환성
            title: ticket.performanceTitle || ticket.title || '',
            artist: ticket.artist || '', // 백엔드에서 artist 필드 받기
            venue: ticket.theater || ticket.venue || '',
            seat: ticket.seat || '', // 백엔드에서 seat 필드 받기
            performedAt: performedAt,
            genre: genre || '', // Ticket 타입에 string 필수
            status: ticket.isPublic ? TicketStatus.PUBLIC : TicketStatus.PRIVATE,
            images: images,
            review: ticket.reviewText ? {
              reviewText: ticket.reviewText,
              createdAt: ticket.createdAt ? new Date(ticket.createdAt) : new Date(),
            } : undefined,
            createdAt: ticket.createdAt ? new Date(ticket.createdAt) : new Date(),
            updatedAt: ticket.updatedAt ? new Date(ticket.updatedAt) : new Date(),
            bookingSite: '',
          };
        });
        
        if (__DEV__) {
          console.log('✅ 티켓 데이터 변환 완료:', {
            원본개수: ticketsList.length,
            변환개수: tickets.length,
            티켓들: tickets.slice(0, 2), // 처음 2개만 로그
          });
        }
        
        set(myTicketsStateAtom, apiStateHelpers.setSuccess(currentState, tickets));
        return ResultFactory.success(tickets);
      } else {
        const errorMessage = result.error?.message || '티켓 목록을 불러오는데 실패했습니다';
        set(myTicketsStateAtom, apiStateHelpers.setError(currentState, errorMessage));
        return ResultFactory.failure(result.error!);
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다';
      set(myTicketsStateAtom, apiStateHelpers.setError(currentState, errorMessage));
      return ResultFactory.failure({ message: errorMessage, code: 'UNKNOWN_ERROR' });
    }
  }
);

/**
 * 친구 티켓 조회
 */
export const fetchFriendTicketsAtom = atom(
  null,
  async (get, set, friendId: string, force: boolean = false) => {
    const currentMapState = get(friendTicketsMapStateAtom);
    const currentMap = currentMapState.data || new Map();
    
    // 캐시 확인
    if (!force && currentMap.has(friendId) && isCacheValid(currentMapState.lastFetch)) {
      return ResultFactory.success(currentMap.get(friendId)!);
    }

    // 로딩 상태는 전체 맵에 대해 설정하지 않고 개별적으로 처리
    try {
      // 백엔드가 공개 티켓만 반환하므로 필터링 불필요
      const result = await ticketService.getFriendTickets(friendId, 0, 100);
      
      if (result.success && result.data) {
        // 백엔드 응답 형식: 배열로 직접 반환 (공개 티켓만)
        const ticketsList = Array.isArray(result.data) ? result.data : [];
        
        // 백엔드 응답을 Ticket 형식으로 변환 (내 티켓 조회와 동일한 로직)
        const convertedTickets: Ticket[] = ticketsList.map((ticket: any) => {
          // viewDate를 Date로 변환
          const performedAt = ticket.viewDate ? new Date(ticket.viewDate) : new Date();
          
          // genre를 백엔드 형식에서 프론트엔드 형식으로 변환
          let genre: string | null = null;
          if (ticket.genre) {
            const genreMap: Record<string, string> = {
              'BAND': '밴드',
              'MUSICAL': '연극/뮤지컬',
              'PLAY': '연극/뮤지컬',
            };
            genre = genreMap[ticket.genre] || ticket.genre;
          }
          
          // 이미지 URL 처리 (resolveImageUrl 사용)
          const images: string[] = [];
          if (ticket.imageUrl) {
            const resolvedUrl = resolveImageUrl(ticket.imageUrl);
            if (resolvedUrl) {
              images.push(resolvedUrl);
            }
          }
          if (ticket.posterUrl) {
            const resolvedUrl = resolveImageUrl(ticket.posterUrl);
            if (resolvedUrl) {
              images.push(resolvedUrl);
            }
          }
          
          const userId = ticket.userId || friendId;
          
          return {
            id: String(ticket.id || ''),
            user_id: userId, // Ticket 타입에 필수
            userId: userId, // 하위 호환성
            title: ticket.performanceTitle || ticket.title || '',
            artist: ticket.artist || '',
            venue: ticket.theater || ticket.venue || '',
            seat: ticket.seat || '',
            performedAt: performedAt,
            genre: genre || '', // Ticket 타입에 string 필수
            status: ticket.isPublic ? TicketStatus.PUBLIC : TicketStatus.PRIVATE,
            images: images,
            review: ticket.reviewText ? {
              reviewText: ticket.reviewText,
              createdAt: ticket.createdAt ? new Date(ticket.createdAt) : new Date(),
            } : undefined,
            createdAt: ticket.createdAt ? new Date(ticket.createdAt) : new Date(),
            updatedAt: ticket.updatedAt ? new Date(ticket.updatedAt) : new Date(),
            bookingSite: '',
          };
        });
        
        const newMap = new Map(currentMap);
        newMap.set(friendId, convertedTickets);
        
        set(friendTicketsMapStateAtom, apiStateHelpers.setSuccess(currentMapState, newMap));
        return ResultFactory.success(convertedTickets);
      } else {
        const errorMessage = result.error?.message || '친구 티켓을 불러오는데 실패했습니다';
        console.error('❌ 친구 티켓 조회 실패:', errorMessage);
        return ResultFactory.failure(result.error!);
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다';
      console.error('❌ 친구 티켓 조회 예외:', error);
      return ResultFactory.failure({ message: errorMessage, code: 'UNKNOWN_ERROR' });
    }
  }
);

/**
 * 티켓 생성
 */
export const createTicketAtom = atom(
  null,
  async (get, set, ticketData: {
    title: string;
    performedAt: Date;
    venue: string;
    artist: string;
    bookingSite?: string;
    genre?: string | null;
    status: TicketStatus;
    review?: {
      reviewText: string;
      createdAt: Date;
      updatedAt?: Date;
    };
    images?: string[];
  }) => {
    try {
      // 낙관적 업데이트: 새 티켓을 목록에 임시 추가
      const currentState = get(myTicketsStateAtom);
      if (currentState.data) {
        const optimisticTicket: Ticket = {
          id: `temp_${Date.now()}`,
          ...ticketData,
          genre: ticketData.genre || null,
          performedAt: ticketData.performedAt,
          userId: 'current_user',
          createdAt: new Date(),
          review: ticketData.review ? {
            reviewText: ticketData.review.reviewText,
            createdAt: ticketData.review.createdAt || new Date(),
          } : undefined,
          images: ticketData.images || [],
        };
        
        const updatedTickets = [optimisticTicket, ...currentState.data];
        set(myTicketsStateAtom, apiStateHelpers.setSuccess(currentState, updatedTickets));
      }

      // 실제 API 호출
      const result = await ticketService.createTicket({
        ...ticketData,
        performedAt: ticketData.performedAt.toISOString(),
      });
      
      if (result.success) {
        // 성공 시 티켓 목록 새로고침
        set(fetchMyTicketsAtom, true);
        return result;
      } else {
        // 실패 시 낙관적 업데이트 롤백
        set(fetchMyTicketsAtom, true);
        return result;
      }
    } catch (error) {
      // 에러 시 롤백
      set(fetchMyTicketsAtom, true);
      const errorMessage = error instanceof Error ? error.message : '티켓 생성에 실패했습니다';
      return ResultFactory.failure({ message: errorMessage, code: 'CREATE_TICKET_ERROR' });
    }
  }
);

/**
 * 티켓 수정
 */
export const updateTicketAtom = atom(
  null,
  async (get, set, ticketData: {
    id: string;
    title?: string;
    performedAt?: Date;
    venue?: string;
    artist?: string;
    seat?: string;
    bookingSite?: string;
    status?: TicketStatus;
    genre?: string | null;
    review?: {
      reviewText: string;
      createdAt: Date;
      updatedAt?: Date;
    };
    images?: string[];
    posterUrl?: string | null;
    imageUrl?: string | null;
    imagePrompt?: string | null;
  }) => {
    console.log('✏️ updateTicketAtom 호출됨');
    console.log('✏️ 수정할 티켓 데이터:', ticketData);
    
    // 티켓 ID 확인
    if (!ticketData.id) {
      const errorMessage = '티켓 ID가 없습니다.';
      console.error('❌ 티켓 ID 없음:', ticketData);
      return ResultFactory.failure({ message: errorMessage, code: 'INVALID_TICKET_ID' });
    }
    
    try {
      // 사용자 ID 가져오기
      const userProfile = get(userProfileAtom);
      const userId = userProfile?.id || userProfile?.user_id;
      
      console.log('✏️ 사용자 프로필:', userProfile);
      console.log('✏️ 사용자 ID:', userId);
      console.log('✏️ 티켓 ID:', ticketData.id);
      
      if (!userId) {
        const errorMessage = '사용자 ID를 가져올 수 없습니다. 다시 로그인해주세요.';
        console.error('❌ 사용자 ID 없음');
        return ResultFactory.failure({ message: errorMessage, code: 'USER_NOT_FOUND' });
      }

      // 장르를 백엔드 형식으로 변환
      const mapGenreToBackend = (genre: string | null | undefined): string => {
        if (!genre) return 'MUSICAL'; // 기본값
        const genreMap: Record<string, string> = {
          '밴드': 'BAND',
          '연극/뮤지컬': 'MUSICAL',
          '뮤지컬': 'MUSICAL',
          '연극': 'PLAY',
        };
        return genreMap[genre] || 'MUSICAL';
      };

      // viewDate 형식으로 변환 (YYYY-MM-DD)
      const viewDate = ticketData.performedAt 
        ? ticketData.performedAt.toISOString().split('T')[0]
        : undefined;

      // 이미지 URL 처리 (첫 번째 이미지만 사용)
      let imageUrl: string | null = null;
      if (ticketData.images && ticketData.images.length > 0) {
        const firstImage = ticketData.images[0];
        if (firstImage.startsWith('http://localhost:8080')) {
          imageUrl = firstImage.replace('http://localhost:8080', '');
        } else if (firstImage.startsWith('/uploads/')) {
          imageUrl = firstImage;
        } else {
          imageUrl = firstImage;
        }
      } else if (ticketData.imageUrl) {
        imageUrl = ticketData.imageUrl;
      }

      // 백엔드 요청 데이터 생성
      const requestData: {
        performanceTitle?: string;
        venue?: string;
        seat?: string;
        artist?: string;
        posterUrl?: string | null;
        genre?: string;
        viewDate?: string;
        imageUrl?: string | null;
        imagePrompt?: string | null;
        reviewText?: string | null;
        isPublic?: boolean;
      } = {};

      // 값이 있는 필드만 추가
      if (ticketData.title !== undefined) {
        requestData.performanceTitle = ticketData.title;
      }
      if (ticketData.venue !== undefined) {
        requestData.venue = ticketData.venue;
      }
      if (ticketData.seat !== undefined) {
        requestData.seat = ticketData.seat;
      }
      if (ticketData.artist !== undefined) {
        requestData.artist = ticketData.artist;
      }
      if (ticketData.posterUrl !== undefined) {
        requestData.posterUrl = ticketData.posterUrl;
      }
      if (ticketData.genre !== undefined) {
        requestData.genre = mapGenreToBackend(ticketData.genre);
      }
      if (viewDate) {
        requestData.viewDate = viewDate;
      }
      if (imageUrl !== null) {
        requestData.imageUrl = imageUrl;
      }
      if (ticketData.imagePrompt !== undefined) {
        requestData.imagePrompt = ticketData.imagePrompt;
      }
      if (ticketData.review?.reviewText !== undefined) {
        requestData.reviewText = ticketData.review.reviewText || null;
      }
      if (ticketData.status !== undefined) {
        requestData.isPublic = ticketData.status === TicketStatus.PUBLIC;
      }

      console.log('✏️ 백엔드 요청 데이터:', JSON.stringify(requestData, null, 2));

      // 낙관적 업데이트
      const currentState = get(myTicketsStateAtom);
      if (currentState.data) {
        const updatedTickets = currentState.data.map(ticket => {
          if (ticket.id === ticketData.id) {
            return {
              ...ticket,
              ...ticketData,
              performedAt: ticketData.performedAt || ticket.performedAt,
              updatedAt: new Date(),
            };
          }
          return ticket;
        });
        
        set(myTicketsStateAtom, apiStateHelpers.setSuccess(currentState, updatedTickets));
        console.log('✏️ 낙관적 업데이트 완료');
      }

      // 실제 API 호출
      console.log('✏️ API 호출 시작: ticketService.updateTicket');
      const result = await ticketService.updateTicket(ticketData.id, userId, requestData);
      console.log('✏️ API 호출 결과:', result);
      
      if (result.success) {
        console.log('✅ 티켓 수정 성공');
        // 성공 시 티켓 목록 새로고침
        set(fetchMyTicketsAtom, true);
        return result;
      } else {
        console.error('❌ 티켓 수정 실패:', result.error);
        // 실패 시 롤백
        set(fetchMyTicketsAtom, true);
        return result;
      }
    } catch (error) {
      console.error('❌ 티켓 수정 중 예외 발생:', error);
      // 에러 시 롤백
      set(fetchMyTicketsAtom, true);
      const errorMessage = error instanceof Error ? error.message : '티켓 수정에 실패했습니다';
      return ResultFactory.failure({ message: errorMessage, code: 'UPDATE_TICKET_ERROR' });
    }
  }
);

/**
 * 티켓 삭제
 */
export const deleteTicketAtom = atom(
  null,
  async (get, set, ticketId: string) => {
    console.log('🗑️ deleteTicketAtom 호출됨');
    console.log('🗑️ 삭제할 티켓 ID:', ticketId);
    try {
      // 사용자 ID 가져오기
      const userProfile = get(userProfileAtom);
      const userId = userProfile?.id || userProfile?.user_id;
      
      console.log('🗑️ 사용자 프로필:', userProfile);
      console.log('🗑️ 사용자 ID:', userId);
      
      if (!userId) {
        const errorMessage = '사용자 ID를 가져올 수 없습니다. 다시 로그인해주세요.';
        console.error('❌ 사용자 ID 없음');
        return ResultFactory.failure({ message: errorMessage, code: 'USER_NOT_FOUND' });
      }

      // 낙관적 업데이트: 티켓 목록에서 제거
      const currentState = get(myTicketsStateAtom);
      if (currentState.data) {
        const updatedTickets = currentState.data.filter(ticket => ticket.id !== ticketId);
        set(myTicketsStateAtom, apiStateHelpers.setSuccess(currentState, updatedTickets));
        console.log('🗑️ 낙관적 업데이트 완료, 남은 티켓 수:', updatedTickets.length);
      }

      // 실제 API 호출
      console.log('🗑️ API 호출 시작: ticketService.deleteTicket');
      const result = await ticketService.deleteTicket(ticketId, userId);
      console.log('🗑️ API 호출 결과:', result);
      
      if (result.success) {
        console.log('✅ 티켓 삭제 성공');
        return result;
      } else {
        console.error('❌ 티켓 삭제 실패:', result.error);
        // 실패 시 롤백
        set(fetchMyTicketsAtom, true);
        return result;
      }
    } catch (error) {
      console.error('❌ 티켓 삭제 중 예외 발생:', error);
      // 에러 시 롤백
      set(fetchMyTicketsAtom, true);
      const errorMessage = error instanceof Error ? error.message : '티켓 삭제에 실패했습니다';
      return ResultFactory.failure({ message: errorMessage, code: 'DELETE_TICKET_ERROR' });
    }
  }
);

/**
 * 날짜별 티켓 조회
 */
export const fetchTicketsByDateAtom = atom(
  null,
  async (get, set, date: string) => {
    try {
      const result = await ticketService.getTicketsByDate(date);
      
      if (result.success && result.data) {
        return ResultFactory.success(result.data);
      } else {
        return ResultFactory.failure(result.error!);
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '날짜별 티켓 조회에 실패했습니다';
      return ResultFactory.failure({ message: errorMessage, code: 'FETCH_BY_DATE_ERROR' });
    }
  }
);

/**
 * 읽기 전용 atoms (컴포넌트에서 사용)
 */
export const myTicketsAtom = atom<Ticket[]>((get) => {
  const state = get(myTicketsStateAtom);
  return state.data || [];
});

export const friendTicketsMapAtom = atom<Map<string, Ticket[]>>((get) => {
  const state = get(friendTicketsMapStateAtom);
  return state.data || new Map();
});

/**
 * 로딩 상태 atoms
 */
export const myTicketsLoadingAtom = atom<boolean>((get) => {
  const state = get(myTicketsStateAtom);
  return state.loading === 'loading';
});

/**
 * 에러 상태 atoms
 */
export const myTicketsErrorAtom = atom<string | null>((get) => {
  const state = get(myTicketsStateAtom);
  return state.error;
});

