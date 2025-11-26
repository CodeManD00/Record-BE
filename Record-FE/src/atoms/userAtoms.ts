// src/atoms/userAtoms.ts
import { atom } from 'jotai';
import { UserProfile } from '../types/user';

/**
 * 로그인한 사용자 프로필 상태
 * 서버에서 /users/me 로 불러온 값을 저장한다.
 */
export const userProfileAtom = atom<UserProfile | null>(null);

/**
 * 인증 상태 (accessToken 저장 등)
 * 로그인/로그아웃 시 관리
 */
export const userAuthAtom = atom<{
  isLoggedIn: boolean;
  accessToken: string | null;
}>({
  isLoggedIn: false,
  accessToken: null,
});

/**
 * 프로필 업데이트 atom
 * updateUserProfile(payload) 호출 후, Jotai에서도 즉시 반영하기 위함
 */
export const updateUserProfileAtom = atom(
  null,
  (get, set, update: Partial<UserProfile>) => {
    const current = get(userProfileAtom);
    if (!current) return;

    const newProfile = { ...current, ...update };
    set(userProfileAtom, newProfile);

    return newProfile;
  }
);

/**
 * 사용자 전체 데이터 초기화
 * 로그아웃 / 회원탈퇴 시 호출됨
 */
export const resetUserDataAtom = atom(null, (get, set) => {
  set(userProfileAtom, null);
  set(userAuthAtom, {
    isLoggedIn: false,
    accessToken: null,
  });
});
