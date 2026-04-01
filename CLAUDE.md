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

## 보안 설정
- 공개 엔드포인트: `/api/auth/**`, `/api/snippets/**`
- 나머지 인증 필요 (Bearer JWT)
- BCrypt 비밀번호 인코딩
- CORS: `CORS_ALLOWED_ORIGINS` 환경변수

## Flyway 마이그레이션
V1~V9까지 적용. `src/main/resources/db/migration/` 참조.

## 환경변수
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` - DB 접속
- `ALADIN_API_KEY` - 알라딘 API
- `KYOBO_PARTNER_ID`, `YES24_PARTNER_ID` - 제휴 마케팅
- `CORS_ALLOWED_ORIGINS` - CORS 허용 origin
- `GOOGLE_CLOUD_PROJECT_ID` - Google Cloud 프로젝트 ID (OCR)
- `GOOGLE_APPLICATION_CREDENTIALS` - Google Cloud 인증 JSON 파일 경로 (OCR)

## 컨벤션
- API URL에 하이픈(-) 사용 금지 (camelCase 또는 소문자 연결)
- 테이블명: `*_tb` 접미사
- 컬럼명: `{테이블약어}_{이름}`
- JPA ddl-auto: `validate` (스키마 변경은 Flyway로만)
- 서버 포트: 8008
