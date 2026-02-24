# Docker 배포 가이드

## 📋 목차
- [빠른 시작](#빠른-시작)
- [Makefile 명령어](#makefile-명령어)
- [상세 가이드](#상세-가이드)
- [서버 배포](#서버-배포)
- [문제 해결](#문제-해결)

## 🚀 빠른 시작

### 1. Docker Hub에 배포하기

```bash
# 1. Docker 로그인
make docker-login

# 2. 빌드 및 푸시 (한 번에)
make deploy
```

### 2. 로컬에서 실행하기

```bash
# 빌드 및 실행 (한 번에)
make quick-deploy

# 또는 단계별로
make gradle-build      # Gradle 빌드
make docker-build      # Docker 이미지 빌드
make docker-run        # 컨테이너 실행
```

### 3. 로그 확인

```bash
# 로그 보기
make docker-logs

# 실시간 로그 보기
make docker-logs-f
```

## 📚 Makefile 명령어

### 전체 명령어 목록 보기
```bash
make help
```

### Gradle 명령어

| 명령어 | 설명 |
|--------|------|
| `make gradle-build` | Gradle로 프로젝트 빌드 (테스트 제외) |
| `make gradle-clean` | Gradle 빌드 정리 |
| `make gradle-test` | 테스트 실행 |
| `make gradle-bootrun` | Spring Boot 애플리케이션 로컬 실행 |

### Docker 빌드 명령어

| 명령어 | 설명 |
|--------|------|
| `make docker-build` | Docker 이미지 빌드 |
| `make docker-build-nc` | Docker 이미지 빌드 (캐시 없이) |

### Docker 실행 명령어

| 명령어 | 설명 |
|--------|------|
| `make docker-run` | Docker 컨테이너 실행 |
| `make docker-stop` | Docker 컨테이너 중지 및 제거 |
| `make docker-restart` | Docker 컨테이너 재시작 |
| `make docker-logs` | 컨테이너 로그 보기 |
| `make docker-logs-f` | 컨테이너 로그 실시간 보기 |
| `make docker-exec` | 컨테이너 내부 쉘 접속 |

### Docker Compose 명령어

| 명령어 | 설명 |
|--------|------|
| `make compose-up` | docker-compose로 서비스 시작 |
| `make compose-down` | docker-compose 서비스 중지 |
| `make compose-logs` | docker-compose 로그 보기 |

### Docker Hub 명령어

| 명령어 | 설명 |
|--------|------|
| `make docker-login` | Docker Hub 로그인 |
| `make docker-push` | Docker Hub에 이미지 푸시 |
| `make docker-pull` | Docker Hub에서 이미지 풀 |

### 정리 명령어

| 명령어 | 설명 |
|--------|------|
| `make docker-clean` | Docker 컨테이너 및 이미지 제거 |
| `make clean-all` | 모든 빌드 파일 정리 (Gradle + Docker) |

### 배포 명령어

| 명령어 | 설명 |
|--------|------|
| `make deploy` | 빌드 → 푸시 → 배포 준비 (완전 배포) |
| `make quick-deploy` | 빌드 → 로컬 실행 (빠른 테스트) |
| `make rebuild` | 재빌드 및 재시작 |

### 유틸리티 명령어

| 명령어 | 설명 |
|--------|------|
| `make status` | Docker 상태 확인 |
| `make health` | 애플리케이션 헬스체크 |

## 📖 상세 가이드

### Docker 이미지 정보

- **이미지 이름**: `kobums/snippetback`
- **태그**:
  - `latest` - 최신 버전
  - `v1.0.0` - 특정 버전
- **포트**: 8008

### 환경 변수

Docker 컨테이너 실행 시 다음 환경 변수를 설정할 수 있습니다:

```bash
SPRING_PROFILES_ACTIVE=prod                    # 프로파일 설정
TZ=Asia/Seoul                                   # 타임존 설정
SPRING_DATASOURCE_URL=jdbc:mysql://...         # DB URL
SPRING_DATASOURCE_USERNAME=gym                 # DB 사용자명
SPRING_DATASOURCE_PASSWORD=gymdb               # DB 비밀번호
```

### 볼륨 마운트

Firebase 서비스 계정 파일을 외부에서 주입하려면:

```bash
docker run -d \
  --name snippetback-app \
  -p 8008:8008 \
  -v ./firebase-service-account.json:/app/config/firebase-service-account.json:ro \
  kobums/snippetback:latest
```

## 🖥️ 서버 배포

### 방법 1: Makefile 사용 (서버에 Makefile 복사)

```bash
# 1. 서버에 Makefile과 docker-compose.yml 복사
scp Makefile docker-compose.yml user@server:/path/to/app/

# 2. 서버에서 실행
ssh user@server
cd /path/to/app
make docker-pull
make docker-run
```

### 방법 2: Docker Compose 사용

```bash
# 1. 서버에 docker-compose.yml 복사
scp docker-compose.yml user@server:/path/to/app/

# 2. 서버에서 실행
ssh user@server
cd /path/to/app
docker-compose pull
docker-compose up -d
```

### 방법 3: 직접 Docker 명령어 사용

```bash
# 서버에서 직접 실행
docker pull kobums/snippetback:latest

docker run -d \
  --name snippetback-app \
  -p 8008:8008 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e TZ=Asia/Seoul \
  --restart unless-stopped \
  kobums/snippetback:latest
```

## 🔧 문제 해결

### 컨테이너가 시작되지 않을 때

```bash
# 로그 확인
make docker-logs

# 또는
docker logs snippetback-app
```

### 이미지 빌드 실패 시

```bash
# 캐시 없이 다시 빌드
make docker-build-nc
```

### 포트가 이미 사용 중일 때

```bash
# 8006 포트를 사용하는 프로세스 확인
lsof -i :8006

# 또는 다른 포트로 실행
docker run -d --name snippetback-app -p 9008:8008 kobums/snippetback:latest
```

### 컨테이너 완전 초기화

```bash
# 모든 관련 리소스 정리
make docker-clean

# 완전 재시작
make clean-all
make quick-deploy
```

### 헬스체크 실패 시

```bash
# 애플리케이션 상태 확인
make health

# 또는 직접 확인
curl http://localhost:8008/actuator/health
```

## 📊 모니터링

### 컨테이너 리소스 사용량 확인

```bash
docker stats snippetback-app
```

### 컨테이너 상세 정보

```bash
docker inspect snippetback-app
```

### 실행 중인 프로세스

```bash
docker top snippetback-app
```

## 🔄 업데이트 프로세스

### 로컬 개발 → 배포

```bash
# 1. 코드 수정 후
make deploy

# 2. 서버에서 업데이트
ssh user@server
docker pull kobums/snippetback:latest
docker stop snippetback-app
docker rm snippetback-app
docker run -d --name snippetback-app -p 8008:8008 kobums/snippetback:latest
```

### 빠른 롤백

```bash
# 특정 버전으로 롤백
docker run -d --name snippetback-app -p 8008:8008 kobums/snippetback:v1.0.0
```

## 📝 베스트 프랙티스

1. **항상 버전 태그 사용**: `latest` 외에 버전 태그도 함께 푸시
2. **로그 모니터링**: 정기적으로 로그 확인
3. **헬스체크 활용**: 자동 헬스체크로 컨테이너 상태 모니터링
4. **리소스 제한**: 프로덕션에서는 CPU/메모리 제한 설정
5. **백업**: 중요한 데이터는 볼륨으로 관리

## 🔐 보안 고려사항

- Firebase 서비스 계정 파일은 절대 Git에 커밋하지 않기
- 환경 변수로 민감한 정보 관리
- 프로덕션에서는 반드시 HTTPS 사용
- 정기적인 이미지 업데이트로 보안 패치 적용
