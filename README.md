# Owl's Pick (통합 게임 큐레이션 및 AI 어시스턴트 플랫폼)

> 다중 플랫폼(Steam, IGDB, ITAD, HLTB) 데이터 통합 파이프라인과 pgvector 기반 RAG 엔진을 적용한 지능형 게임 추천 백엔드 서비스

## 1. 프로젝트 개요
- 기간: 2026.01 ~ 2026.08
- 인원: 1
- 목적
  - 플랫폼별로 파편화된 게임 메타데이터(가격, 플레이타임, 평가) 통합 수집 및 정규화
  - LLM 및 벡터 유사도 검색(pgvector)을 통한 사용자 맞춤형 대화형 게임 탐색 지원

## 2. 시스템 아키텍처
<img width="1318" height="1014" alt="Owl&#39;s Pick Backend drawio" src="https://github.com/user-attachments/assets/8201996a-99d1-460c-9997-1c537aedc6d5" />

## 3. 기술 스택

| 분류 | 기술 스택 | 적용 목적 및 세부 내용 |
| :--- | :--- | :--- |
| **Language & Framework** | Language & Framework | 메인 백엔드 비즈니스 로직 및 GenAI 비동기 파이프라인 전담 분리 |
| **Database & ORM** | PostgreSQL, Redis, Spring Data JPA, QueryDSL, Flyway | RDB 정규화 및 캐싱 계층 분리, 타입 세이프 쿼리 최적화 및 DB 형상 관리 |
| **AI & Search** | Gemini API, pgvector, pg_trgm | 메타데이터/리뷰 NLP 요약, 768차원 코사인 유사도 추론 및 GIN 부분일치 색인 |
| **Infra & DevOps** | AWS (EC2, RDS, Parameter Store, IAM), Docker, Docker Compose, GitHub Actions, Nginx | 컨테이너 기반 격리 환경 구성, CI/CD 자동화 및 리버스 프록시 라우팅 |
| **Monitoring & Alerting** | Prometheus, Grafana, Loki, Micrometer | 메트릭 및 구조화 JSON 로그 수집, 대시보드 시각화 및 시스템 관측 가능성(Observability) 확보 |
| **Security & Auth** | Spring Security, OAuth 2.0 (OIDC), JWT | 무상태(Stateless) 토큰 인증 및 소셜 로그인 통합 인가 체계 구축 |
| **External API & Docs** | Steam, IGDB, ITAD, HowLongToBeat, Firebase Cloud Messaging (FCM), Swagger (OpenAPI) | 무상태(Stateless) 토큰 인증 및 소셜 로그인 통합 인가 체계 구축 |
| **Library & Testing** | Resilience4j, Pydantic, Testcontainers, JUnit5 | 외부 API 호출 서킷 브레이커 장애 격리, 데이터 스키마 유효성 검증 및 격리 환경 통합 테스트 |

