# Money Diary API (Spring Boot)

용돈기입장 백엔드 API 서버입니다. **Spring Boot(Java) + Firestore + Firebase Authentication** 으로 구성되어 있고, **Cloud Run** 배포를 전제로 만들어졌습니다.

- Spring Boot 3.4 (Java 17)
- Firebase Admin SDK 초기화 → `Firestore`, `FirebaseAuth` 빈 제공
- `Authorization: Bearer <Firebase ID Token>` 검증 Security 필터
- 거래(들어온 돈/나간 돈)·카테고리 CRUD (`users/{uid}/...` 서브컬렉션)
- 공개 헬스체크 `GET /health`, 인증 확인용 `GET /me`

## 사전 준비

1. GCP 프로젝트 + Firestore(Native 모드) 데이터베이스 생성
2. Firebase Authentication 사용 설정
3. 로컬 자격 증명(둘 중 하나):
   - `gcloud auth application-default login` (권장)
   - 또는 서비스 계정 키 발급 후 `export GOOGLE_APPLICATION_CREDENTIALS=/path/service-account.json`

### 환경 변수

| 변수 | 설명 | 예시 |
| --- | --- | --- |
| `GCP_PROJECT_ID` | GCP 프로젝트 ID | `money-diary-498803` |
| `GCP_FIRESTORE_DATABASE_ID` | Firestore 데이터베이스 ID. **named DB** 면 그 이름, 기본 DB 면 생략(=`(default)`) | `moneydiary` |

> ⚠️ Firestore 프로젝트 ID는 표시 이름과 다를 수 있습니다(예: 표시 이름 `money-diary` → 실제 ID `money-diary-498803`).
> `gcloud projects list` 로 실제 ID 를 확인하세요.
> 또한 기본 DB(`(default)`)가 아니라 이름이 있는 데이터베이스를 만들었다면 반드시 `GCP_FIRESTORE_DATABASE_ID` 를 지정해야 합니다.

## 로컬 실행

```bash
cd server
export GCP_PROJECT_ID=money-diary-498803
export GCP_FIRESTORE_DATABASE_ID=moneydiary   # named DB 인 경우
./gradlew bootRun
```

헬스체크:

```bash
curl http://localhost:8080/health
# {"status":"ok"}
```

## Firestore 연결 스모크 테스트

ID 토큰 없이 리포지토리를 통해 실제 Firestore 에 write→read→delete 를 수행해 연결을 검증합니다.
(`smoketest` 프로파일에서만 동작하며, 끝나면 앱이 종료됩니다.)

```bash
cd server
GCP_PROJECT_ID=money-diary-498803 \
GCP_FIRESTORE_DATABASE_ID=moneydiary \
./gradlew bootRun --args='--spring.profiles.active=smoketest'
# [SMOKE] ✅ Firestore 연결/저장/조회/삭제 모두 성공
```

## API 엔드포인트

모든 `/api/**` 요청에는 `Authorization: Bearer <Firebase ID Token>` 헤더가 필요합니다.
데이터는 토큰의 UID 기준으로 `users/{uid}` 하위에 저장됩니다.

### 거래 (Transactions) — `/api/transactions`

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/transactions` | 목록 (날짜 내림차순) |
| GET | `/api/transactions/{id}` | 단건 조회 |
| POST | `/api/transactions` | 생성 (201) |
| PUT | `/api/transactions/{id}` | 수정 |
| DELETE | `/api/transactions/{id}` | 삭제 (204) |

요청 본문:

```json
{
  "type": "EXPENSE",        // INCOME | EXPENSE
  "amount": 1500,
  "category": "간식",
  "date": "2026-06-08",     // yyyy-MM-dd
  "memo": "초콜릿"
}
```

### 카테고리 (Categories) — `/api/categories`

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/categories` | 목록 |
| POST | `/api/categories` | 생성 (201) |
| PUT | `/api/categories/{id}` | 수정 |
| DELETE | `/api/categories/{id}` | 삭제 (204) |

요청 본문:

```json
{ "name": "간식", "type": "EXPENSE" }   // type 은 선택(null 허용)
```

### 호출 예시

```bash
curl http://localhost:8080/api/transactions \
  -H "Authorization: Bearer <FIREBASE_ID_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"type":"EXPENSE","amount":1500,"category":"간식","date":"2026-06-08","memo":"초콜릿"}'
```

## Firestore 인덱스

거래 목록 조회는 `date` + `createdAt` 두 필드로 정렬하므로 **복합 인덱스**가 필요합니다.
정의는 `firestore.indexes.json` 에 있으며, 생성 방법:

```bash
# Firebase CLI 사용 시
firebase deploy --only firestore:indexes

# 또는 gcloud
gcloud firestore indexes composite create \
  --collection-group=transactions \
  --field-config=field-path=date,order=descending \
  --field-config=field-path=createdAt,order=descending \
  --database=moneydiary \
  --project=money-diary-498803
```

## 빌드

```bash
cd server
./gradlew clean bootJar
java -jar build/libs/money-diary-api-0.0.1-SNAPSHOT.jar
```

## Cloud Run 배포

```bash
cd server

gcloud run deploy money-diary-api \
  --source . \
  --region asia-northeast3 \
  --allow-unauthenticated \
  --set-env-vars GCP_PROJECT_ID=money-diary-498803,GCP_FIRESTORE_DATABASE_ID=moneydiary
```

### 권한

Cloud Run 런타임 서비스 계정에 Firestore 접근 권한이 필요합니다:

```bash
gcloud projects add-iam-policy-binding money-diary-498803 \
  --member="serviceAccount:<RUNTIME_SA>@money-diary-498803.iam.gserviceaccount.com" \
  --role="roles/datastore.user"
```

> `--allow-unauthenticated` 는 Cloud Run 엔드포인트를 공개한다는 의미이며, 실제 사용자 인증은 애플리케이션 레벨의 Firebase ID 토큰 검증으로 처리됩니다.

## 구조

```
server/
├── build.gradle / settings.gradle
├── Dockerfile
├── firestore.indexes.json                # 복합 인덱스 정의
└── src/main/java/com/moneydiary/api/
    ├── MoneyDiaryApiApplication.java
    ├── config/FirebaseConfig.java         # Firestore(named DB 지원) + FirebaseAuth 빈
    ├── domain/                            # Transaction, Category, TransactionType
    ├── repository/                        # Firestore 접근 (users/{uid}/...)
    ├── security/                          # Firebase ID 토큰 검증
    ├── smoke/FirestoreSmokeTest.java      # smoketest 프로파일 전용 연결 검증
    └── web/                               # 컨트롤러 + DTO + 예외 핸들러
```

## 다음 단계 (참고)

- 주간/월간 통계, 캘린더용 집계 엔드포인트
- 입력 검증 강화(jakarta validation) 및 페이징
- Android(WebView) 클라이언트가 Firebase 로그인 후 ID 토큰으로 API 호출하도록 연동
