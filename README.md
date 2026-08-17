# Owl's Pick (통합 게임 큐레이션 및 AI 어시스턴트 플랫폼)

> 다중 플랫폼(Steam, IGDB, ITAD, HLTB) 데이터 통합 파이프라인과 `pgvector` 기반 RAG 엔진을 적용한 지능형 게임 추천 백엔드 서비스

<br/>
<br/>

## 1. 프로젝트 개요
- 서비스 목적 : 다중 플랫폼(Steam, IGDB, ITAD, HLTB)에 파편화된 게임 메타데이터를 통합 정규화하고, LLM 및 벡터 유사도 검색을 통해 개인화된 게임 큐레이션 및 대화형 탐색 지원
- 개발 인원 및 기간: 1인 백엔드 개발 / 2026.01 ~ 2026.08
- 상세 레퍼런스: [서비스 기획 및 요구사항 명세](https://app.notion.com/p/2ae4b8ce5c838089807fff7be0cf7d9c) | [외부 API 분석 및 연동 명세](https://app.notion.com/p/API-2ae4b8ce5c8380309193e17af6721d1c)

<br/>
<br/>

## 2. 시스템 아키텍처

<br/>

<p align="center">
  <img width="850" alt="Owl&#39;s Pick Backend drawio" src="https://github.com/user-attachments/assets/8201996a-99d1-460c-9997-1c537aedc6d5" />
</p>

<br/>
<br/>

## 3. 기술 스택

| 분류 | 기술 스택 | 적용 목적 및 세부 내용 |
| :--- | :--- | :--- |
| **Language & Framework** | Language & Framework | 메인 백엔드 비즈니스 로직 및 GenAI 비동기 파이프라인 전담 분리 |
| **Database & ORM** | PostgreSQL, Redis, Spring Data JPA, QueryDSL, Flyway | RDB 정규화 및 캐싱 계층 분리, 타입 세이프 쿼리 최적화 및 DB 형상 관리 |
| **AI & Search** | OpenAI API, pgvector, pg_trgm | 메타데이터 한글화, 스팀 리뷰 요약, 1536차원 벡터 유사도 검색 및 GIN 부분일치 색인 |
| **Infra & DevOps** | AWS (EC2, RDS, Parameter Store, IAM), Docker, Docker Compose, GitHub Actions, Nginx | 컨테이너 기반 격리 환경 구성, CI/CD 자동화 및 리버스 프록시 라우팅 |
| **Monitoring & Alerting** | Prometheus, Grafana, Loki, Micrometer | 메트릭 및 구조화 JSON 로그 수집, 대시보드 시각화 및 시스템 관측 가능성 확보 |
| **Security & Auth** | Spring Security, OAuth 2.0 (OIDC), JWT | 무상태 토큰 인증 및 소셜 로그인 통합 인가 체계 구축 |
| **External API & Docs** | Steam, IGDB, ITAD, HowLongToBeat, Firebase Cloud Messaging (FCM), Swagger (OpenAPI) | 다중 게임 플랫폼 메타데이터 연동, 푸시 알림 트리거 및 인터랙티브 API 명세화 |
| **Library & Testing** | Resilience4j, Pydantic, Testcontainers, JUnit5 | 외부 API 호출 서킷 브레이커 장애 격리, 데이터 스키마 검증 및 격리 통합 테스트 |

<br/>
<br/>

## 4. 핵심 기능 및 구현 내용

### 1) 멀티 플랫폼 게임 데이터 수집 파이프라인
- Steam, IGDB, ITAD, HLTB 4개 외부 API를 연동하여 약 17만 건의 게임 메타데이터 통합 적재
- 수동 동기화 컨트롤러와 스케줄러로 수집 경로를 이원화하고, Virtual Thread 및 Resilience4j를 적용해 Rate Limit 및 네트워크 지연 방어

### 2) 소셜 로그인 및 인증 체계 (OAuth 2.0 & JWT)
- Google, Kakao, Naver 3사 OAuth 2.0 소셜 로그인 및 무상태 JWT 발급 체계 구축
- 웹훅 서명 검증을 통한 외부 연동 해제 처리 및 개발/테스트 편의를 위한 관리자 바이패스 로그인 구현

### 3) 다차원 큐레이션 및 하이브리드 검색
- 사용자 선호 태그 기반 맞춤형 추천과 탐색형 추천을 결합한 7가지 테마 큐레이션 제공
- PostgreSQL 배열 연산자(`@>`, `&&`) 및 `pg_trgm`의 `similarity` 함수를 QueryDSL에 등록하여 키워드 유사도 검색 및 오타 교정 구현

### 4) 위시리스트 및 가격 변동 비동기 알림
- 위시리스트 등록 게임의 할인 감지 시 FCM 기반 타겟팅 푸시 알림 발송
- Spring 비동기 이벤트(`@Async`)를 적용해 메인 트랜잭션과 알림 발송 로직을 분리하고, 무효 토큰 자동 삭제 처리

### 5) 사용자 온보딩 및 연령 제한 필터링
- 신규 가입 시 생년월일과 선호 태그를 수집하여 개인화 추천 기반 마련
- 글로벌 심의 등급과 메타데이터 키워드를 통합 분석해 성인용 콘텐츠 자동 판별 및 연령별 접근 제어/썸네일 블러 처리

### 6) LLM 파이프라인 및 Owl's 챗봇 (FastAPI 연계)
- `gpt-4o-mini` 기반 메타데이터 한글화 및 스팀 리뷰 요약/장단점 키워드 추 파이프라인 구축
- `text-embedding-3-small` 임베딩 기반 코사인 유사도 검색과 대화 맥락 기반 독립 검색어 추출을 적용한 대화형 게임 추천 RAG 시스템 구현
- 비동기 처리 실패 건을 `genai_failed_task` 테이블에 `trace_id`와 함께 적재하여 재시도 스케줄러를 통한 복구 지원

### 7) 클라우드 인프라 및 CI/CD 배포
- AWS EC2 환경에서 Docker Compose로 Spring Boot와 FastAPI를 분산 배포하고 Nginx 리버스 프록시로 라우팅
- AWS Parameter Store와 IAM을 연동해 보안 자격 증명을 주입하고, GitHub Actions 및 BuildKit 캐시를 활용해 CI/CD 파이프라인 구축

### 8) 시스템 관측성(Observability) 및 통합 모니터링
- Prometheus, Grafana, Loki를 연동하여 JVM/커넥션 풀 메트릭 및 구조화 JSON 로그를 실시간 수집/시각화
- Grafana Alert Rule을 기반으로 `ERROR` 레벨 로그 감지 시 Slack 웹훅으로 원인 및 로그 링크 자동 발송

<br/>
<br/>

## 5. 데이터베이스 모델링

<br/>

<p align="center">
  <img width="850" alt="Owl&#39;s Pick-ERD" src="https://github.com/user-attachments/assets/3232b3e4-ba83-484e-bc39-043c8e60b74d" />
</p>

<br/>

- `game` 엔티티 중심의 다중 플랫폼 메타데이터 정규화 및 수집 주기별 격리 구조 설계
- 코사인 유사도 기반 RAG 검색을 위한 `pgvector`(`VECTOR(1536)`) 및 부분 일치 검색 가속을 위한 `pg_trgm` GIN 인덱스 적용
- 상세 문서: [데이터베이스 설계 및 도메인 모델링](https://app.notion.com/p/DB-2a94b8ce5c8380d48d6dcb62d6fb0d28)

<br/>
<br/>

## 6. 실행 방법 (Getting Started)

### 1) 사전 요구사항
- JDK 25
- Docker Desktop

<br/>

### 2) 환경 변수 설정
- 루트 디렉터리의 `.env.example` 복사 후 `.env` 파일 생성 및 필수 값 정의.
  
```bash
cp .env.example .env
```

```properties
# .env 주요 설정 항목 예시
DB_HOST=localhost
POSTGRES_PASSWORD=your_postgres_password
REDIS_HOST=localhost

JWT_SECRET=your_jwt_secret_key
INITIALIZATION_ADMIN_KEY=your_admin_key

GOOGLE_CLIENT_ID=your_id
KAKAO_CLIENT_ID=your_id
NAVER_CLIENT_ID=your_id

STEAM_WEB_API_KEY=your_steam_key
IGDB_CLIENT_ID=your_igdb_id
IGDB_CLIENT_SECRET=your_igdb_secret
ITAD_WEB_API_KEY=your_itad_key

FASTAPI_URL=http://localhost:8000
INTERNAL_API_BASE_URL=http://localhost:8080
FIREBASE_CREDENTIALS_BASE64=your_fcm_base64_json
```

<br/>

### 3) 로컬 인프라 구동
- Docker Desktop 실행 후 로컬 개발용 DB 및 캐시 컨테이너 백그라운드 구동.

```bash
docker compose up -d
```

<br/>

### 4) 애플리케이션 빌드 및 실행

- Spring Boot 백엔드 로컬 실행
```bash
./gradlew clean bootRun
```
<br/>

### 5) 구동 확인 및 API 명세
* Swagger API 명세서: `http://localhost:8080/swagger-ui/index.html`
* Health Check Endpoint:`http://localhost:8080/actuator/health`







