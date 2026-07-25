# Snippet Backend (Spring Boot)

## 기술 스택
- **Spring Boot 3.5.0** / Java 21 / Gradle
- **MariaDB** + JPA/Hibernate + Flyway 마이그레이션
- **Spring Security + JWT** (jjwt 0.11.5)
- **Google Cloud Vision API** (OCR 텍스트 추출)
- JSoup (웹 크롤링), Spring dotenv, Lombok

## 패키지 구조

```
com.snippet
├── config/          # SecurityConfig, WebConfig
├── controller/      # REST API 컨트롤러 (8개)
├── service/         # 비즈니스 로직 (10개)
├── repository/      # JPA Repository
├── entity/          # 도메인 엔티티 (4개)
├── dto/             # 요청/응답 DTO
├── security/        # JWT 인증 (Filter, Provider, UserDetails)
├── exception/       # GlobalExceptionHandler
└── util/            # AffiliateLinkGenerator
```

## 엔티티

| 엔티티 | 테이블 | 주요 필드 |
|--------|--------|-----------|
| Book | `book_tb` | isbn, title, author, coverUrl, affiliateUrl, publisher, totalPage, category |
| Snippet | `record_tb` | book(FK), user(FK), type(snippet/diary/review), text, tag, relatedPage |
| User | `user_tb` | email(unique), password, name |
| UserBook | `userbook_tb` | user(FK), book(FK), type(wish/have/borrow/return), status(none/waiting/reading/completed/dropped), readPage, startDate, endDate |
| RefreshToken | `refreshtoken_tb` | user(FK), token(unique, 불투명 랜덤 문자열), expireDate — 기기별 1행, refresh 시 회전 없이 만료만 연장(sliding) |

## API 엔드포인트

### SnippetController (`/api/snippets`)
- `GET /cards` - 스와이프용 카드 조회 (count, excludeIds)
- `GET /archive` - 아카이브 스니펫 조회 (ids)

### AuthController (`/api/auth`)
- `POST /register` - 회원가입
- `POST /login` - 로그인 (JWT 발급)

### BookController (`/api/books`)
- `GET /search` - 알라딘 API 도서 검색 (query, page)
- `GET /` / `GET /{id}` / `POST /` / `PUT /{id}` / `PATCH /{id}` / `DELETE /{id}`

### UserBookController (`/api/userbooks`)
- CRUD: `GET /` / `GET /{id}` / `POST /` / `PUT /{id}` / `PATCH /{id}` / `DELETE /{id}`
- `GET /monthly` - 월별 도서 조회 (year, month)
- `GET /stats/monthly` - 월별 통계
- `GET /stats/yearly` - 연간 통계
- `GET /stats/category` - 카테고리별 통계
- `GET /stats/insights` - 독서 인사이트

### RecordController (`/api/records`)
- CRUD: `GET /` / `GET /{id}` / `POST /` / `PUT /{id}` / `PATCH /{id}` / `DELETE /{id}`
- `GET /bybook` - 도서별 기록 (bookId, type)
- `GET /monthly` - 월별 기록 (type, year, month)

### UserController (`/api/users`)
- CRUD: `GET /` / `GET /{id}` / `POST /` / `PUT /{id}` / `PATCH /{id}` / `DELETE /{id}`

### AdminCrawlerController (`/api/admin/crawl`)
- `POST /aladin` - 알라딘 문장 크롤링

### OcrController (`/api/ocr`)
- `POST /` - Google Cloud Vision API를 통한 OCR 텍스트 추출

### AppVersionController (`/api/appversion`)
- `GET /?platform=ios|android&version=1.0.28` - 앱 버전 정책 조회 (인증 불필요)
- 버전 비교는 서버가 전담하고 클라이언트는 `updateRequired`/`updateAvailable`만 사용
- **fail-open**: platform/version 미전달·형식 오류, min 미설정·형식 오류, storeUrl 미설정이면
  전부 "차단 안 함"으로 응답 (설정 실수로 전 사용자가 잠기는 사고 방지)

## 보안 설정
- 공개 엔드포인트: `/api/auth/**`, `/api/snippets/**`, `/api/appversion`
- 나머지 인증 필요 (Bearer JWT)
- BCrypt 비밀번호 인코딩
- CORS: `CORS_ALLOWED_ORIGINS` 환경변수

## Flyway 마이그레이션
V1~V30까지 적용. `src/main/resources/db/migration/` 참조.

⚠️ `user_tb`를 참조하는 FK를 새로 추가할 때는 반드시 `ON DELETE CASCADE`(또는 의도적으로
`SET NULL`)를 명시할 것. 기본값 RESTRICT로 두면 회원 탈퇴가 전부 실패한다 (V30에서 수정).

## 환경변수
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` - DB 접속
- `JWT_SECRET` - JWT 서명 시크릿 (Base64, 프로덕션 필수 — 미설정 시 로컬 개발용 기본값 사용)
- `ALADIN_API_KEY` - 알라딘 API
- `KYOBO_PARTNER_ID`, `YES24_PARTNER_ID` - 제휴 마케팅
- `CORS_ALLOWED_ORIGINS` - CORS 허용 origin
- `GOOGLE_CLOUD_PROJECT_ID` - Google Cloud 프로젝트 ID (OCR)
- `GOOGLE_APPLICATION_CREDENTIALS` - Google Cloud 인증 JSON 파일 경로 (OCR)
- `APP_MIN_VERSION_IOS`, `APP_MIN_VERSION_ANDROID` - 이 버전 미만이면 앱 사용 차단 (강제 업데이트).
  **비워두면 강제 업데이트 비활성** — 올리는 순간 그보다 낮은 버전 사용자는 전부 앱에서 잠기므로 주의
- `APP_LATEST_VERSION_IOS`, `APP_LATEST_VERSION_ANDROID` - 최신 버전 (권장 업데이트 안내용, 스킵 가능)
- `APP_STORE_URL_IOS`, `APP_STORE_URL_ANDROID` - 스토어 이동 URL (기본값 있음)
- `APP_UPDATE_MESSAGE` - 업데이트 안내 문구 (미설정 시 클라이언트 기본 문구)

## 컨벤션
- API URL에 하이픈(-) 사용 금지 (camelCase 또는 소문자 연결)
- 테이블명: `*_tb` 접미사
- 컬럼명: `{테이블약어}_{이름}`
- JPA ddl-auto: `validate` (스키마 변경은 Flyway로만)
- 서버 포트: 8008
