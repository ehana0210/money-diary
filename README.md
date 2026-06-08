# 용돈기입장 (Money Diary)

Android WebView 앱으로 동작하는 초등학생용 용돈 기입장입니다.  
들어온 돈·나간 돈 내역은 **백엔드 API(Spring Boot on Cloud Run) → Firestore**에 저장됩니다.  
앱은 별도 회원가입 없이 **Firebase 익명 인증**으로 자동 로그인하며, 기기별 사용자(UID)로 데이터가 분리됩니다.

## 구조

```
app/src/main/
├── assets/          # WebView에서 로드하는 HTML/CSS/JS
│   ├── index.html
│   ├── styles.css
│   └── app.js       # 백엔드 /api/* 호출 (Firebase ID 토큰 사용)
├── java/.../MainActivity.kt   # WebView + Firebase 익명 인증 + 토큰 브리지
└── res/

server/              # Spring Boot API 서버 (Cloud Run). server/README.md 참고
```

데이터 흐름: `WebView(app.js)` → Firebase ID 토큰 첨부 → `Cloud Run(Spring Boot)` → `Firestore (users/{uid}/...)`

- Android 네이티브가 `FirebaseAuth.signInAnonymously()` 로 받은 ID 토큰을 JS 브리지(`AndroidBridge`)로 WebView 에 전달
- `app.js` 가 `Authorization: Bearer <token>` 헤더로 백엔드 호출
- 배포 URL: `https://money-diary-api-223320053383.asia-northeast3.run.app`

## 기능

- 들어온 돈 / 나간 돈 등록 (날짜, 금액, 카테고리, 메모)
- 잔액·들어온 돈·나간 돈 합계 표시
- 내역 필터 (전체 / 들어온 돈 / 나간 돈) + **5개씩 페이징**
- **주간 / 월간 통계** (카테고리별 나간 돈)
- **캘린더** (날짜별 내역)
- **카테고리 추가·삭제**
- 커스텀 확인 팝업
- 백엔드(Firestore) 저장, Firebase 익명 인증으로 사용자별 데이터 분리

## 실행 방법

1. [Android Studio](https://developer.android.com/studio)에서 이 폴더를 **Open**
2. Gradle Sync 완료 후 에뮬레이터 또는 실제 기기에서 **Run**

WebView 설정 (`MainActivity.kt`):

- `javaScriptEnabled = true`
- `allowUniversalAccessFromFileURLs = true` ← file:// 페이지에서 원격 API 호출 허용
- `addJavascriptInterface(AuthBridge(), "AndroidBridge")` ← 익명 인증 ID 토큰 전달

> 사전 준비: Firebase 콘솔에서 **익명 인증(Anonymous) 활성화** + `app/google-services.json` 배치가 필요합니다.

## 웹 UI 수정

`app/src/main/assets/` 아래 HTML/CSS/JS 파일을 수정한 뒤 앱을 다시 빌드하면 반영됩니다.  
별도 웹서버 없이 Android assets에 파일만 넣으면 됩니다.
