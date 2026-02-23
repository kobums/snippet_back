# Snippet-Back (스니펫 백엔드)

📖 **Snippet** 프로젝트의 백엔드 API 서버입니다. 
책갈피나 영감을 주는 문구(Snippet)를 저장하고 제공하는 코어 비즈니스 로직을 담당합니다.

## ✨ 주요 기능

*   **RESTful API 제공**: 프론트엔드 클라이언트가 데이터를 조회하고 조작할 수 있는 API 엔드포인트.
*   **스니펫 관리**: 새로운 스니펫 추가, 조회 및 관리 기능 (Spring Data JPA 활용).
*   **데이터베이스 마이그레이션**: Flyway를 이용한 안정적인 데이터베이스 스키마 버전 관리.

## 🛠️ 기술 스택 (Tech Stack)

*   **언어**: Java 21
*   **프레임워크**: Spring Boot 3.5.0
*   **데이터베이스**: MariaDB
*   **ORM / 데이터 접근**: Spring Data JPA
*   **DB 마이그레이션**: Flyway
*   **빌드 도구**: Gradle

## 🚀 시작하기 (Getting Started)

### 요구 사항

*   Java 21 이상
*   MariaDB 실행 중 (설정 파일에 맞게 세팅)

### 로컬 환경에서 실행

1.  **환경 변수 설정**: `.env` 파일에 데이터베이스 연결 정보 등 필요한 환경 변수를 세팅합니다.
2.  **프로젝트 빌드 및 실행**:
    ```bash
    # 저장소 클론 후 back 디렉토리로 이동
    cd back

    # Gradle 래퍼를 이용해 Spring Boot 앱 실행
    ./gradlew bootRun
    ```
3.  앱이 기본 포트(일반적으로 8080)에서 실행됩니다. (`http://localhost:8080`)

## 📂 프로젝트 구조

*   `src/main/java/com/snippet/`: 애플리케이션의 핵심 Java 소스 코드 (Controller, Service, Repository, Entity, Dto 등)
*   `src/main/resources/`: 스프링 환경 설정 파일(`application.yml` 등) 및 DB 마이그레이션 스크립트(`db/migration`)

## 📝 API 문서 (참고)

*   추가적인 API 명세는 `docs` 폴더의 Postman 컬렉션을 참고해 주세요.
