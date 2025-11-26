/**
 * API 호출을 위한 기본 설정 및 에러 처리 (완전 수정본)
 */
export const API_BASE_URL = __DEV__
  ? 'http://localhost:8080'
  : 'https://api.ticketbook.app';


import { Result, ResultFactory, ErrorFactory } from '../../utils/result';
import AsyncStorage from '@react-native-async-storage/async-storage';

// API 기본 설정

const API_TIMEOUT = 20000; // 20초

// API 에러 타입
export interface ApiError {
  code: string;
  message: string;
  details?: any;
}

// API 응답 타입
export interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
  error?: ApiError;
  message?: string;
}

// ----------------------
// ApiClient 클래스
// ----------------------
class ApiClient {
  private authToken: string | null = null;

  constructor() {
    this.loadTokenFromStorage();
  }

  /**
   * ⭐ 외부에서 토큰 재로딩이 필요할 때 호출
   */
  async ensureAuthToken() {
    await this.loadTokenFromStorage();
  }

  /**
   * ⭐ AsyncStorage에서 토큰 불러오기
   * 메모리에 이미 토큰이 있으면 스킵 (성능 최적화)
   */
  private async loadTokenFromStorage() {
    // 이미 메모리에 토큰이 있으면 스킵
    if (this.authToken) {
      if (__DEV__) {
        console.log('🔑 Token already in memory (길이:', this.authToken.length, ')');
      }
      return;
    }

    try {
      const token = await AsyncStorage.getItem('authToken');
      if (token) {
        this.authToken = token;
        if (__DEV__) {
          console.log('🔑 Token loaded from storage (길이:', token.length, ')');
        }
      } else {
        if (__DEV__) {
          console.warn('⚠️ No token found in storage');
        }
      }
    } catch (e) {
      console.warn('Failed to load auth token', e);
    }
  }

  /**
   * ⭐ 토큰 저장
   */
  async setAuthToken(token: string) {
    this.authToken = token;
    try {
      await AsyncStorage.setItem('authToken', token);
      if (__DEV__) console.log('🔐 Token saved to storage');
    } catch (e) {
      console.warn('Failed to save token', e);
    }
  }

  /**
   * ⭐ 토큰 제거
   */
  async clearAuthToken() {
    this.authToken = null;
    try {
      await AsyncStorage.removeItem('authToken');
      if (__DEV__) console.log('🗑️ Token removed from storage');
    } catch (e) {
      console.warn('Failed to remove token', e);
    }
  }

  /**
   * ⭐ 저장된 토큰 가져오기 (외부에서 사용)
   */
  async getStoredToken(): Promise<string | null> {
    // 이미 메모리에 토큰이 있으면 반환
    if (this.authToken) {
      return this.authToken;
    }

    // AsyncStorage에서 토큰 불러오기
    try {
      const token = await AsyncStorage.getItem('authToken');
      if (token) {
        this.authToken = token;
        if (__DEV__) console.log('🔑 Token loaded from storage');
      }
      return token;
    } catch (e) {
      console.warn('Failed to load auth token', e);
      return null;
    }
  }

  /**
   * ⭐ 기본 헤더 (Content-Type 강제 제거)
   */
  private getHeaders(
    customHeaders?: Record<string, string>,
    skipAuth: boolean = false
  ): Record<string, string> {
    const headers: Record<string, string> = {
      ...(customHeaders || {}),
    };

    // ❗ multipart 요청 때는 Content-Type 자동 생성 → 절대 강제 지정하면 안됨

    // Authorization 적용 (skipAuth가 false일 때만)
    if (!skipAuth && this.authToken) {
      headers.Authorization = `Bearer ${this.authToken}`;
      if (__DEV__) {
        console.log('🔑 Authorization 헤더 추가됨 (토큰 길이:', this.authToken.length, ')');
      }
    } else {
      if (__DEV__) {
        if (skipAuth) {
          console.log('🔓 인증 불필요 엔드포인트 - Authorization 헤더 제외');
        } else {
          console.warn('⚠️ Authorization 헤더 없음 - authToken이 null입니다');
        }
      }
    }

    return headers;
  }

  /**
   * ⭐ JWT 토큰이 필요 없는 엔드포인트 목록
   * 인증이 필요한 엔드포인트는 제외해야 함
   */
  private readonly noAuthEndpoints = [
    '/auth/login',
    '/auth/signup',
    '/auth/forgot-id',
    '/auth/forgot/temporary-password',
    '/auth/password/change', // 이전 비밀번호만으로 변경 가능하도록 설정
    '/auth/email/send-code',
    '/auth/email/verify',
    '/users/nickname',
    '/users/me/profile-image',
    '/stt/transcribe-and-save',
  ];

  /**
   * ⭐ 엔드포인트가 인증이 필요한지 확인
   */
  private needsAuth(url: string): boolean {
    return !this.noAuthEndpoints.some(endpoint => url.startsWith(endpoint));
  }

  /**
   * ⭐ 내부 공통 요청 처리
   */
  private async request<T>(
    url: string,
    options: RequestInit = {},
    timeoutMs: number = API_TIMEOUT
  ): Promise<Result<T>> {
    try {
      // 인증이 필요한 엔드포인트만 토큰 로딩
      const needsAuth = this.needsAuth(url);
      if (needsAuth) {
        await this.loadTokenFromStorage();
      }

      const fullUrl = url.startsWith('http') ? url : `${API_BASE_URL}${url}`;

      // 헤더 생성 (토큰 포함 여부 확인)
      const headers = this.getHeaders(
        options.headers as Record<string, string>,
        !needsAuth // skipAuth 플래그 전달
      );

      if (__DEV__) {
        console.log(`API Request: ${options.method || 'GET'} ${fullUrl}`);
        console.log('📤 요청 헤더:', {
          'Content-Type': headers['Content-Type'] || '자동 설정',
          'Authorization': headers['Authorization'] ? 'Bearer ***' : '없음',
          ...Object.keys(headers)
            .filter(key => !['Content-Type', 'Authorization'].includes(key))
            .reduce((acc, key) => {
              acc[key] = headers[key];
              return acc;
            }, {} as Record<string, string>),
        });
        if (options.body) {
          try {
            const bodyStr = typeof options.body === 'string' ? options.body : JSON.stringify(options.body);
            console.log('📤 요청 Body:', bodyStr.substring(0, 200) + (bodyStr.length > 200 ? '...' : ''));
          } catch (e) {
            console.log('📤 요청 Body: (파싱 불가)');
          }
        }
      }

      // 타임아웃 설정
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

      let response: Response;
      try {
        response = await fetch(fullUrl, {
          ...options,
          headers,
          signal: controller.signal,
        });
      } catch (fetchError: any) {
        clearTimeout(timeoutId);
        if (__DEV__) {
          console.error('❌ fetch 요청 실패:', fetchError);
          console.error('에러 이름:', fetchError?.name);
          console.error('에러 메시지:', fetchError?.message);
          if (fetchError?.name === 'AbortError') {
            console.error('⏱️ 요청 타임아웃 발생');
          } else if (fetchError?.message?.includes('Network')) {
            console.error('🌐 네트워크 연결 오류');
          }
        }
        throw fetchError;
      }

      clearTimeout(timeoutId);

      const contentType = response.headers.get('content-type');
      const responseText = await response.text();

      if (__DEV__) {
        console.log(`API Response: ${response.status} ${fullUrl}`);
      }

      // JSON 응답 처리
      if (contentType?.includes('application/json')) {
        let data: any;

        try {
          data = JSON.parse(responseText);
        } catch (e) {
          return ResultFactory.failure(
            ErrorFactory.api('PARSE_ERROR', 'JSON 파싱 실패: ' + responseText)
          );
        }

        // 200 응답이고 success 필드가 있는 경우 (ApiResponseObject 형태)
        if (response.ok && data.success !== undefined) {
          if (data.success) {
            return ResultFactory.success(data.data as T);
          } else {
            // success가 false인 경우 에러 처리
            if (__DEV__) {
              console.log('⚠️ API 응답 success=false:', data);
            }
            return this.handleHttpError(response.status, data as ApiResponse<T>, fullUrl);
          }
        }

        // 200 응답이지만 success 필드가 없는 경우 (직접 데이터 반환)
        if (response.ok) {
          if (__DEV__) {
            console.log('✅ API 응답 (success 필드 없음, 직접 데이터 반환):', data);
          }
          return ResultFactory.success(data as T);
        }

        // 에러 응답 처리
        if (__DEV__) {
          console.log('❌ 에러 응답 수신:', {
            status: response.status,
            statusText: response.statusText,
            data: data,
            rawResponse: responseText.substring(0, 500),
          });
        }
        return this.handleHttpError(response.status, data as ApiResponse<T>, fullUrl);
      }

      // JSON 아니면 그냥 텍스트 반환
      if (response.ok) {
        return ResultFactory.success(responseText as T);
      }

      return this.handleHttpError(response.status, { error: responseText }, fullUrl);
    } catch (error: any) {
      return this.handleError(error);
    }
  }

  // ----------------------
  // ✔ JSON 전송 요청
  // ----------------------

  async get<T>(
    url: string,
    config?: { timeoutMs?: number; headers?: Record<string, string> }
  ): Promise<Result<T>> {
    return this.request<T>(
      url,
      {
        method: 'GET',
        headers: config?.headers,
      },
      config?.timeoutMs
    );
  }

  async post<T>(
    url: string,
    data?: any,
    options?: { headers?: Record<string, string>; timeoutMs?: number }
  ): Promise<Result<T>> {
    return this.request<T>(url, {
      method: 'POST',
      body: data ? JSON.stringify(data) : undefined,
      headers: {
        'Content-Type': 'application/json',
        ...(options?.headers || {}),
      },
    }, options?.timeoutMs);
  }

  async put<T>(
    url: string,
    data?: any,
    options?: { headers?: Record<string, string>; timeoutMs?: number }
  ): Promise<Result<T>> {
    return this.request<T>(url, {
      method: 'PUT',
      body: data ? JSON.stringify(data) : undefined,
      headers: {
        'Content-Type': 'application/json',
      },
    }, options?.timeoutMs);
  }

  async patch<T>(
    url: string,
    data?: any,
    options?: { headers?: Record<string, string>; timeoutMs?: number }
  ): Promise<Result<T>> {
    return this.request<T>(url, {
      method: 'PATCH',
      body: data ? JSON.stringify(data) : undefined,
      headers: {
        'Content-Type': 'application/json',
        ...(options?.headers || {}),
      },
    }, options?.timeoutMs);
  }

  async delete<T>(
    url: string,
    data?: any,
    options?: { headers?: Record<string, string>; timeoutMs?: number }
  ): Promise<Result<T>> {
    return this.request<T>(url, {
      method: 'DELETE',
      body: data ? JSON.stringify(data) : undefined,
      headers: data 
        ? { 
            'Content-Type': 'application/json',
            ...(options?.headers || {}),
          }
        : (options?.headers || {}),
    }, options?.timeoutMs);
  }

  // ----------------------
  // ✔ FormData 전송 요청
  // ----------------------

  async postForm<T>(
    url: string,
    formData: FormData,
    config?: { timeoutMs?: number }
  ): Promise<Result<T>> {
    return this.request<T>(url, {
      method: 'POST',
      body: formData,
      headers: {}, // Content-Type 자동 생성
    }, config?.timeoutMs);
  }

  async putForm<T>(
    url: string,
    formData: FormData,
    config?: { timeoutMs?: number }
  ): Promise<Result<T>> {
    return this.request<T>(url, {
      method: 'PUT',
      body: formData,
      headers: {},
    }, config?.timeoutMs);
  }

  async patchForm<T>(
    url: string,
    formData: FormData,
    config?: { timeoutMs?: number }
  ): Promise<Result<T>> {
    return this.request<T>(url, {
      method: 'PATCH',
      body: formData,
      headers: {},
    }, config?.timeoutMs);
  }

  // ----------------------
  // 에러 처리
  // ----------------------

  private handleHttpError<T>(status: number, data: ApiResponse<T> | any, url?: string): Result<T> {
    // 로그인 API는 인증이 필요 없으므로 401이 발생하면 아이디/비밀번호 오류로 처리
    const isLoginEndpoint = url?.includes('/auth/login');
    
    if (status === 401) {
      const hadToken = !!this.authToken;
      
      // 로그인 엔드포인트가 아니면 토큰 제거
      if (!isLoginEndpoint) {
        this.clearAuthToken();
        if (__DEV__) {
          console.warn('🔒 Unauthorized - token cleared');
          console.warn('토큰이 있었는지:', hadToken ? '있었음' : '없었음');
          if (hadToken) {
            console.warn('토큰이 있었지만 401 에러 발생 - 토큰이 만료되었거나 유효하지 않을 수 있습니다');
          }
        }
      } else {
        if (__DEV__) {
          console.warn('🔒 로그인 API 401 - 아이디/비밀번호가 올바르지 않을 수 있습니다');
        }
      }
    }

    // 에러 상세 로깅
    if (__DEV__) {
      console.error(`❌ HTTP ${status} 에러 발생`);
      console.error('📍 에러 발생 URL:', url || '알 수 없음');
      console.error('에러 응답 데이터:', JSON.stringify(data, null, 2));
      
      // 500 에러의 경우 백엔드 스택 트레이스 출력
      if (status === 500) {
        console.error('⚠️ 서버 내부 오류 발생 - 백엔드 로그 확인 필요');
        if (data?.trace) {
          console.error('백엔드 스택 트레이스:', data.trace.substring(0, 500) + (data.trace.length > 500 ? '...' : ''));
        }
        if (data?.error) {
          console.error('에러 타입:', data.error);
        }
      }
      
      if (isLoginEndpoint) {
        console.error('📍 로그인 API 엔드포인트 - 401은 아이디/비밀번호 오류일 수 있습니다');
        console.error('📋 백엔드 응답 상세:', {
          error: data?.error,
          message: data?.message,
          data: data?.data,
          전체응답: data,
        });
      }
    }

    // 에러 메시지 추출 (다양한 형태 지원)
    let errorMessage = '';
    if (typeof data === 'string') {
      errorMessage = data;
    } else if (data?.message) {
      errorMessage = data.message;
    } else if (data?.error?.message) {
      errorMessage = data.error.message;
    } else if (data?.error) {
      errorMessage = typeof data.error === 'string' ? data.error : JSON.stringify(data.error);
    } else {
      errorMessage = `서버 오류가 발생했습니다 (${status})`;
    }

    switch (status) {
      case 400:
        return ResultFactory.failure(ErrorFactory.validation(errorMessage));
      case 401:
        // 로그인 API의 경우 다른 메시지 사용
        if (isLoginEndpoint) {
          return ResultFactory.failure(
            ErrorFactory.unauthorized('아이디 또는 비밀번호가 올바르지 않습니다.')
          );
        }
        return ResultFactory.failure(
          ErrorFactory.unauthorized(
            errorMessage === 'Unauthorized' 
              ? '인증이 만료되었습니다. 다시 로그인해주세요.' 
              : errorMessage
          )
        );
      case 403:
        return ResultFactory.failure(ErrorFactory.forbidden(errorMessage));
      case 404:
        return ResultFactory.failure(ErrorFactory.notFound('리소스', errorMessage));
      case 500:
        if (__DEV__) {
          console.error('🔴 서버 내부 오류 (500)');
          console.error('에러 상세:', data);
        }
        return ResultFactory.failure(ErrorFactory.server(errorMessage));
      default:
        return ResultFactory.failure(
          ErrorFactory.api(`HTTP_${status}`, errorMessage)
        );
    }
  }

  private handleError(error: any): Result<any> {
    if (__DEV__) console.error('API Error:', error);

    if (error.name === 'AbortError') {
      return ResultFactory.failure(ErrorFactory.timeout());
    }
    if (error.message?.includes('Network')) {
      return ResultFactory.failure(ErrorFactory.network());
    }
    return ResultFactory.failure(
      ErrorFactory.unknown(error.message || '알 수 없는 오류 발생')
    );
  }
}

// 싱글톤 인스턴스
export const apiClient = new ApiClient();
export default apiClient;
