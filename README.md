# 🎟️ Re:cord - AI 기반 공연 후기 티켓북

> AI 음성 요약과 이미지 생성을 통해 공연 후기를 기록하고 공유하는 개인화된 디지털 티켓 플랫폼

---

## 🔍 프로젝트 개요

**Re:cord**는 공연 관람 후 사용자의 후기를 기반으로  
✔️ AI 요약 
✔️ 감정 분석 
✔️ 티켓 이미지 생성
까지 지원하는 **개인화된 티켓 기록 서비스**입니다.  

- **공연 후기**를 음성으로 기록하면, 핵심 키워드별로 요약되며  
- **후기에 따라 시각적으로 반영된 티켓 이미지**가 생성됩니다.  
- 유저는 이를 저장하거나 지인과 **공유할 수 있는 디지털 아카이빙 경험**을 누릴 수 있습니다.

---

## 🚀 실행 방법

### 앱 실행 흐름 (사용자)

1. 앱 접속 → **빈 티켓 생성**
2. 텍스트 또는 음성으로 후기 및 공연 정보 입력
3. AI가 후기를 요약하고 감정을 분석하여 **프롬프트 생성**
4. DALL·E 기반으로 **티켓 이미지 생성**
5. 티켓 이미지 보관 및 instagram 공유 가능  

---

## 🧪 주요 코드 예시


```
// 후기 분석 결과를 바탕으로 이미지 프롬프트 생성
String prompt = String.format(
    "A %s musical theater scene about %s,\n" +
    "set in %s and depicting %s,\n" +
    "featuring %s,\n" +
    "with %s and %s,\n" +
    "under %s.\n" +
    "No captions, no text in the image.",
    emotion, theme, setting, relationship, action, character1, character2, lighting
);
```

---

## 🧱 기술 스택


| 분류     | 스택                                 |
| ------ | ---------------------------------- |
| 프론트엔드  | React Native, TypeScript, Expo     |
| 백엔드    | Spring Boot, AWS RDS, S3      |
| AI 기능  | Google STT, OpenAI GPT-4, DALL·E 3 |
| 이미지 생성 | 프롬프트 기반 DALL·E API                 |

---

## 🖼️ 시스템 구성도

![image](https://github.com/user-attachments/assets/c5b24c7c-4907-426f-8cec-40c06d4fa08f)

---

## 📬 문의처

- 팀장 이세연 (syys1944@ewhain.net)
- GitHub Issue 또는 Discussions를 통해 문의 가능
