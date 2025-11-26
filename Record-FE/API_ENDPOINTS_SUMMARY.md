# API 엔드포인트 사용 현황 정리

각 페이지별로 사용하는 API 엔드포인트를 정리한 문서입니다.

---

## 📄 1. LoginPage.tsx

**파일 위치**: `src/pages/auth/LoginPage.tsx`

### 구현 기능
- 로그인 폼 (아이디, 비밀번호)
- 로그인 후 프로필 정보 자동 로드

### 사용 API
- `POST /auth/login`
  - 서비스: `authService.signIn(id, password)`
  - 요청: `{ id: string, password: string }`
  - 응답: `{ token: string, type: string, expiresIn: number, role: string }`
  - 성공 시: 프로필 정보 자동 로드 (`fetchMyProfile(true)`)

---

## 📄 2. SignupPage.tsx

**파일 위치**: `src/pages/auth/SignupPage.tsx`

### 구현 기능
- 회원가입 폼 (아이디, 비밀번호, 이메일, 닉네임)
- 이메일 인증 코드 발송 및 검증

### 사용 API
- `POST /auth/email/send-code`
  - 서비스: `authService.sendSignupVerificationCode(email)`
  - 요청: `{ email: string }`
  - 응답: 성공/실패

- `POST /auth/email/verify`
  - 서비스: `authService.verifySignupCode(email, code)`
  - 요청: `{ email: string, code: string }`
  - 응답: 성공/실패

- `POST /auth/signup`
  - 서비스: `authService.signUp(username, password, email, nickname)`
  - 요청: `{ id: string, password: string, email: string, nickname: string }`
  - 응답: `{ token: string, type: string, expiresIn: number, role: string }`

---

## 📄 3. FindIdPage.tsx

**파일 위치**: `src/pages/auth/recovery/FindIdPage.tsx`

### 구현 기능
- 이메일로 아이디 찾기

### 사용 API
- `POST /auth/forgot-id`
  - 서비스: `authService.findIdByEmail(email)`
  - 요청: `{ email: string }`
  - 응답: 성공 시 이메일로 아이디 전송

---

## 📄 4. FindPasswordPage.tsx

**파일 위치**: `src/pages/auth/recovery/FindPasswordPage.tsx`

### 구현 기능
- 이메일로 임시 비밀번호 발급

### 사용 API
- `POST /auth/forgot/temporary-password`
  - 서비스: `apiClient.post('/auth/forgot/temporary-password', { email })`
  - 요청: `{ email: string }`
  - 응답: 성공 시 이메일로 임시 비밀번호 전송

---

## 📄 5. OCRPage.tsx

**파일 위치**: `src/pages/add-ticket/OCRPage.tsx`

### 구현 기능
- 티켓 이미지 선택 (카메라/갤러리)
- OCR로 티켓 정보 자동 추출

### 사용 API
- `POST /ocr/extract/ticket`
  - 서비스: `ocrService.extractTicket(asset)`
  - 요청: `multipart/form-data` (file: 이미지 파일)
  - 응답: `{ title: string, date: string, time: string, venue: string, seat: string, artist: string }`
  - 성공 시: `AddTicketPage`로 이동하여 추출된 정보 자동 입력

---

## 📄 6. AddReviewPage.tsx

**파일 위치**: `src/pages/add-ticket/AddReviewPage.tsx`

### 구현 기능
- 후기 작성 (텍스트 입력)
- 오디오 파일 업로드 및 STT 변환
- 후기 텍스트 정리 (Organize)
- 후기 요약 (Summarize)
- 장르별 질문 가져오기

### 사용 API
- `GET /review-questions?genre={genre}`
  - 서비스: `apiClient.get('/review-questions?genre={genre}')`
  - 요청: 쿼리 파라미터 `genre` (예: "밴드", "연극/뮤지컬", "COMMON")
  - 응답: `string[]` (질문 배열)
  - 실패 시: 기본 질문 사용

- `POST /stt/transcribe-and-save`
  - 서비스: `sttService.transcribeAndSave(audioUri, fileName, fileType)`
  - 요청: `multipart/form-data` (file: 오디오 파일)
  - 응답: `{ id: number, fileName: string, createdAt: string, transcript: string, summary: string | null, finalReview: string | null }`
  - 성공 시: `reviewText`에 변환된 텍스트 추가

- `POST /review/organize`
  - 서비스: `sttService.organizeReview(text, transcriptionId)`
  - 요청: `{ text: string, transcriptionId?: number }`
  - 응답: `{ id: number, transcript: string, summary: string | null, finalReview: string | null }`
  - 성공 시: `reviewText`를 정리된 텍스트로 교체
  - 타임아웃: 60초

- `POST /review/summarize`
  - 서비스: `sttService.summarizeReview(reviewText, transcriptionId)`
  - 요청: `{ text: string, transcriptionId?: number }`
  - 응답: `{ id: number, transcript: string, summary: string | null, finalReview: string | null }`
  - 성공 시: 요약 텍스트를 모달에 표시
  - 타임아웃: 60초

---

## 📄 7. MyPage.tsx

**파일 위치**: `src/pages/my-page/MyPage.tsx`

### 구현 기능
- 사용자 프로필 표시
- 내 티켓 목록 표시
- 친구 목록 표시

### 사용 API
- `GET /users/me`
  - 서비스: `useUserProfileData()` (hook 내부에서 `fetchMyProfileAtom` 사용)
  - 요청: 없음 (인증 토큰 사용)
  - 응답: `{ id: string, nickname: string, email: string, profileImage?: string, isAccountPrivate: boolean, createdAt: string, updatedAt: string }`
  - 화면 포커스 시: `fetchMyProfile(true)` 자동 호출

---

## 📄 8. PersonalInfoEditPage.tsx

**파일 위치**: `src/pages/my-page/PersonalInfoEditPage.tsx`

### 구현 기능
- 프로필 이미지 변경
- 닉네임, 이메일, 계정 공개 설정 변경

### 사용 API
- `PUT /users/me/profile-image`
  - 서비스: `userService.updateProfileImage(file)`
  - 요청: `multipart/form-data` (file: 이미지 파일)
  - 응답: `UserProfile`
  - 성공 시: `fetchMyProfile()` 자동 호출

- `PUT /users/me`
  - 서비스: `apiClient.put('/users/me', payload)`
  - 요청: `{ nickname: string, email: string, isAccountPrivate: boolean }`
  - 응답: `UserProfile`
  - 성공 시: `userService.fetchMyProfile()` 및 `fetchMyProfile(true)` 호출

---

## 📄 9. SettingsPage.tsx

**파일 위치**: `src/pages/my-page/SettingsPage.tsx`

### 구현 기능
- 설정 메뉴 표시
- 프로필 정보 표시

### 사용 API
- `GET /users/me`
  - 서비스: `useUserProfileData()` (hook 내부에서 `fetchMyProfileAtom` 사용)
  - 요청: 없음 (인증 토큰 사용)
  - 응답: `UserProfile`
  - 화면 포커스 시: `fetchMyProfile(true)` 자동 호출

---

## 📄 10. 기타 페이지들

### AddTicketPage.tsx
- 티켓 정보 입력 폼
- API 호출 없음 (로컬 상태 관리만)

### TicketCompletePage.tsx
- 티켓 생성 완료 화면
- API 호출 없음 (로컬 상태 관리만)

### ImageOptions.tsx
- 이미지 선택 옵션
- `POST /review/summarize` 사용 (후기 요약)

### AIImageResults.tsx
- AI 이미지 생성 결과 표시
- API 호출 없음 (로컬 상태 관리만)

### FriendsListPage.tsx
- 친구 목록 표시
- `GET /friendships/{userId}/friends` 사용

### AddFriendPage.tsx
- 친구 추가
- `POST /friendships/send` 사용 (Header: `X-User-Id`)

### SentRequestsPage.tsx
- 보낸 친구 요청 목록
- `GET /friendships/{userId}/sent-requests` 사용

### HistoryPage.tsx
- 티켓 히스토리
- `GET /api/reviews/me/{userId}` 사용

### CalendarScreen.tsx
- 캘린더 화면
- API 호출 없음 (로컬 상태 관리만)

### MainPage.tsx
- 메인 홈 화면
- API 호출 없음 (로컬 상태 관리만)

---

## 📋 공통 사용 API

### 인증
- 모든 API 요청에 `Authorization: Bearer {token}` 헤더 자동 포함
- 토큰은 `AsyncStorage`에 저장됨

### 에러 처리
- 모든 API 호출은 `Result<T>` 타입 반환
- `result.success`로 성공/실패 판단
- `result.error?.message`로 에러 메시지 확인

### 타임아웃
- 기본 타임아웃: 20초
- STT 관련 API (organize, summarize): 60초

---

## 🔗 관련 서비스 파일

- `src/services/auth/authService.ts` - 인증 관련 API
- `src/services/api/userService.ts` - 사용자 관련 API
- `src/services/api/ticketService.ts` - 티켓/리뷰 관련 API
- `src/services/api/sttService.ts` - STT 관련 API
- `src/services/api/ocrService.ts` - OCR 관련 API
- `src/services/api/friendService.ts` - 친구 관련 API
- `src/services/api/client.ts` - 공통 API 클라이언트

