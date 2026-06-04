# 용돈기입장 (Money Diary)

Android WebView 앱으로 동작하는 초등학생용 용돈 기입장입니다.  
들어온 돈·나간 돈 내역은 WebView의 **localStorage**에 저장되며, 앱을 껐다 켜도 유지됩니다.

## 구조

```
app/src/main/
├── assets/          # WebView에서 로드하는 HTML/CSS/JS
│   ├── index.html
│   ├── styles.css
│   └── app.js
├── java/.../MainActivity.kt   # WebView + domStorageEnabled
└── res/
```

## 기능

- 들어온 돈 / 나간 돈 등록 (날짜, 금액, 카테고리, 메모)
- 잔액·들어온 돈·나간 돈 합계 표시
- 내역 필터 (전체 / 들어온 돈 / 나간 돈) + **5개씩 페이징**
- **주간 / 월간 통계** (카테고리별 나간 돈)
- **캘린더** (날짜별 내역)
- **카테고리 추가·삭제**
- 커스텀 확인 팝업
- localStorage 자동 저장

## 실행 방법

1. [Android Studio](https://developer.android.com/studio)에서 이 폴더를 **Open**
2. Gradle Sync 완료 후 에뮬레이터 또는 실제 기기에서 **Run**

WebView 설정 (`MainActivity.kt`):

- `javaScriptEnabled = true`
- `domStorageEnabled = true` ← localStorage 사용에 필수

## 웹 UI 수정

`app/src/main/assets/` 아래 HTML/CSS/JS 파일을 수정한 뒤 앱을 다시 빌드하면 반영됩니다.  
별도 웹서버 없이 Android assets에 파일만 넣으면 됩니다.
