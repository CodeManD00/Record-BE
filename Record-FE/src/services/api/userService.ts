/**
 * User Service – FIXED VERSION
 */
import { Result, ResultFactory, ErrorFactory } from '../../utils/result';
import { apiClient } from './client';
import { userProfileAtom } from '../../atoms/userAtoms';
import { getDefaultStore } from 'jotai';

const store = getDefaultStore();

export interface UserProfile {
  id: string;
  nickname: string;
  email: string;
  profileImage?: string;
  isAccountPrivate: boolean;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface ChangePasswordData {
  userId: string;
  oldPassword: string;
  newPassword: string;
}

export interface LoginData {
  id: string;
  password: string;
}

export interface RegisterData {
  id: string;
  password: string;
  email: string;
  nickname: string;
}

export interface UpdateProfileData {
  nickname?: string;
  email?: string;
  isAccountPrivate?: boolean;
}

class UserService {
  private profile: UserProfile | null = null;

  async fetchMyProfile(): Promise<Result<UserProfile>> {
    try {
      const result = await apiClient.get<UserProfile>('/users/me');

      if (result.success && result.data) {
        this.profile = result.data;
        store.set(userProfileAtom, result.data);
      }
      return result;
    } catch {
      return ResultFactory.failure(
        ErrorFactory.unknown('프로필 정보를 불러올 수 없습니다.')
      );
    }
  }

  async updateNickname(userId: string, nickname: string): Promise<Result<UserProfile>> {
    try {
      const result = await apiClient.patch<UserProfile>('/users/nickname', {
        userId,
        nickname,
      });

      if (result.success && result.data) {
        this.profile = result.data;
        store.set(userProfileAtom, result.data);
      }

      return result;
    } catch {
      return ResultFactory.failure(
        ErrorFactory.unknown('닉네임 변경 중 오류가 발생했습니다.')
      );
    }
  }

  async updateProfileImage(
    file: {
      uri: string;
      type: string;
      name: string;
    },
    userId: string
  ): Promise<Result<UserProfile>> {

    const formData = new FormData();
    formData.append('file', file as any);
    formData.append('userId', userId);

    const result = await apiClient.putForm<UserProfile>(
      '/users/me/profile-image',
      formData
    );

    if (result.success) {
      // 이미지 업로드 후 전체 프로필 다시 fetch (가장 안정적)
      await this.fetchMyProfile();
      
      // 업로드 성공 시 최신 프로필 반환
      if (result.data) {
        return ResultFactory.success(result.data);
      }
    }

    return result;
  }

  async updateProfile(
    profileData: Partial<UserProfile>
  ): Promise<Result<UserProfile>> {
    try {
      const result = await apiClient.put<UserProfile>('/users/me', profileData);

      if (result.success && result.data) {
        this.profile = result.data;
        store.set(userProfileAtom, result.data);
      }

      return result;
    } catch {
      return ResultFactory.failure(
        ErrorFactory.unknown('프로필 수정 중 오류가 발생했습니다.')
      );
    }
  }

  async changePassword(data: ChangePasswordData): Promise<Result<void>> {
    try {
      // 백엔드가 토큰 없이 이전 비밀번호만으로 변경을 허용하므로
      // noAuthEndpoints에 포함되어 있어 토큰 없이 요청 가능
      const result = await apiClient.post<void>('/auth/password/change', {
        userId: data.userId,
        oldPassword: data.oldPassword,
        newPassword: data.newPassword,
      });

      return result;
    } catch {
      return ResultFactory.failure(
        ErrorFactory.unknown('비밀번호 변경 중 오류가 발생했습니다.')
      );
    }
  }

  async deleteAccount(password: string): Promise<Result<void>> {
    // DELETE body 전달: DeleteAccountRequest { password: string }
    const result = await apiClient.delete<void>(
      '/users/me',
      { password }
    );

    if (result.success) {
      this.clearProfile();
    }

    return result;
  }

  clearProfile() {
    this.profile = null;
    store.set(userProfileAtom, null);
  }

  getProfile(): UserProfile | null {
    return this.profile;
  }
}

export const userService = new UserService();
export default userService;
