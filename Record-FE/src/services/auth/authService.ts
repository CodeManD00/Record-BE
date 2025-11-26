/**
 * Authentication Service – FIXED FINAL VERSION
 */

import { Result, ResultFactory, ErrorFactory } from '../../utils/result';
import { apiClient } from '../api/client';
import { userService } from '../api/userService';

export interface AuthResponse {
  token: string;
  type: string;
  expiresIn: number;
  role: string;
}

class AuthService {
  private accessToken: string | null = null;

  constructor() {
    this.restoreToken();
  }

  private async restoreToken() {
    const token = await apiClient.getStoredToken();
    if (token) {
      this.accessToken = token;
    }
  }

  /** ============================
   *  이메일 인증 코드 발송
   * ============================ */
  async sendSignupVerificationCode(email: string): Promise<Result<any>> {
    const result = await apiClient.post('/auth/email/send-code', { email });

    if (!result.success) {
      return ResultFactory.failure(
        ErrorFactory.badRequest(result.error?.message || '인증 코드 발송 실패')
      );
    }

    return ResultFactory.success(result.data);
  }

  /** ============================
   *  이메일 인증 코드 검증
   * ============================ */
  async verifySignupCode(email: string, code: string): Promise<Result<any>> {
    const result = await apiClient.post('/auth/email/verify', { email, code });

    if (!result.success) {
      return ResultFactory.failure(
        ErrorFactory.badRequest(result.error?.message || '인증 번호가 올바르지 않습니다.')
      );
    }

    return ResultFactory.success(result.data);
  }

  /** ============================
   *  회원가입
   * ============================ */
  async signUp(id: string, password: string, email: string, nickname: string): Promise<Result<AuthResponse>> {
    const result = await apiClient.post<AuthResponse>('/auth/signup', {
      id,
      password,
      email,
      nickname,
    });

    if (!result.success) {
      return ResultFactory.failure(
        ErrorFactory.badRequest(result.error?.message || '회원가입 실패')
      );
    }

    return ResultFactory.success(result.data);
  }

  /** ============================
   *  로그인
   * ============================ */
  async signIn(id: string, password: string): Promise<Result<AuthResponse>> {
    console.log('🔵 signIn 시작:', { id, passwordLength: password.length });
    console.log('🔵 API Base URL:', __DEV__ ? 'http://localhost:8080' : 'https://api.ticketbook.app');
    console.log('🔵 전체 URL:', `${__DEV__ ? 'http://localhost:8080' : 'https://api.ticketbook.app'}/auth/login`);
    
    try {
      const result = await apiClient.post<AuthResponse>('/auth/login', {
        id,
        password,
      });

      console.log('🔵 로그인 API 응답:', {
        success: result.success,
        hasData: !!result.data,
        error: result.error,
      });

      if (!result.success || !result.data) {
        console.error('❌ 로그인 실패:', result.error);
        return ResultFactory.failure(
          ErrorFactory.unauthorized(result.error?.message || '아이디 또는 비밀번호가 올바르지 않습니다.')
        );
      }

      const data = result.data;
      console.log('✅ 로그인 성공, 토큰 저장 중...');
      apiClient.setAuthToken(data.token);
      this.accessToken = data.token;
      console.log('✅ 토큰 저장 완료');

      // 프로필은 로그인 페이지에서 별도로 로드하도록 변경 (중복 호출 방지)

      return ResultFactory.success(data);
    } catch (error) {
      console.error('❌ 로그인 중 예외 발생:', error);
      console.error('에러 타입:', error instanceof Error ? error.constructor.name : typeof error);
      console.error('에러 메시지:', error instanceof Error ? error.message : String(error));
      throw error;
    }
  }

  /** ============================
   *  로그아웃
   * ============================ */
  async signOut() {
    this.accessToken = null;
    apiClient.clearAuthToken();
    userService.clearProfile();
    return ResultFactory.success(undefined);
  }

  /** ============================
   *  이메일로 아이디 찾기
   * ============================ */
  async findIdByEmail(email: string): Promise<Result<any>> {
    const result = await apiClient.post('/auth/forgot-id', { email });

    if (!result.success) {
      return ResultFactory.failure(
        ErrorFactory.badRequest(result.error?.message || '아이디 찾기 실패')
      );
    }

    return ResultFactory.success(result.data);
  }

  /** ============================
   *  임시 비밀번호 발급
   * ============================ */
  async sendTemporaryPassword(email: string): Promise<Result<any>> {
    const result = await apiClient.post('/auth/forgot/temporary-password', { email });

    if (!result.success) {
      return ResultFactory.failure(
        ErrorFactory.badRequest(result.error?.message || '임시 비밀번호 발급 실패')
      );
    }

    return ResultFactory.success(result.data);
  }

  /** ============================
   *  인증 여부
   * ============================ */
  async isAuthenticated(): Promise<boolean> {
    if (this.accessToken) return true;
    const token = await apiClient.getStoredToken();
    return !!token;
  }
}

export const authService = new AuthService();
export default authService;
