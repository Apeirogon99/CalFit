> **Note**
> 해당 저장소는 저의 주요 역할 및 아키텍처 의사결정 과정을 정리하기 위한 요약본입니다.
> 전체적인 정보를 위해 원본을 보고 싶으시다면 [CalFit](https://github.com/coffiiness/BE)를 클릭해주세요.

## 핵심 성과

> **Claude Code + TDD 기반 개발 프로세스**를 설계·도입하여 실질 개발 2.5주 만에 6개 담당 도메인 모듈 구축 + CI/CD 파이프라인 + AWS 배포까지 완료
> 모놀리식에서 출발해 **3단계 아키텍처 진화**를 거치며 모듈 간 직접 의존을 제거하고 유지보수 가능한 구조를 확립

| 항목 | 수치 |
|---|---|
| 본인 기여 | 커밋 151건 · 테스트 269개 작성 (전체 434개 중 62%) |
| 아키텍처 | 모놀리식 → 멀티모듈 → 4계층 + Facade (3단계 진화) |
| 테스트 커버리지 | Line 52.9% · Branch 35.1% (TDD 기반 지속 확장 중) |
| CI/CD | GitHub Actions (EC2) + Jenkins (EKS) 듀얼 파이프라인 |

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [아키텍처 진화](#2-아키텍처-진화)
3. [Claude Code + TDD 개발 프로세스](#3-claude-code--tdd-개발-프로세스)
4. [CI/CD 파이프라인](#4-cicd-파이프라인)

---

## 1. 프로젝트 개요

![Java](https://img.shields.io/badge/Java-17-orange) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green) ![MySQL](https://img.shields.io/badge/MySQL-8.0-blue) ![JPA](https://img.shields.io/badge/JPA-Hibernate-59666C) ![Docker](https://img.shields.io/badge/Docker-Compose-2496ED) ![AWS](https://img.shields.io/badge/AWS-EKS_EC2_S3-FF9900) ![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-CI/CD-2088FF) ![Jenkins](https://img.shields.io/badge/Jenkins-EKS_CD-D24939)

**CalFit**은 기업의 일정 관리, 회의실 예약, 채용 프로세스, 결제/요금제를 통합 관리하는 **SaaS 기반 워크스페이스 플랫폼**입니다.

| 항목 | 내용 |
|---|---|
| 인원 | 백엔드 5명 |
| 역할 | **팀 리드** — 아키텍처 설계 및 의사결정, TDD 방법론 팀 도입, 코드 리뷰 |
| 기간 | 2026.01.22 ~ 03.20 (약 2개월) |

**담당 도메인**: SaaS 멀티테넌시 · 요금제/결제(billing, payment) · 관리자 통계·리포트(report) · 멤버/그룹 권한(member, group) · 워크스페이스(workspace)

**팀 리드 기여**:
- 멀티모듈 → 4계층 → Facade 아키텍처 진화 방향을 설계하고 팀에 공유
- Kent Beck의 Augmented Coding 기반 TDD 프로세스를 수립하고 팀 전체에 적용
- `@CalfitApiTest` + Fixture 패턴 등 테스트 인프라를 구축하여 팀원 5명이 동일한 방식으로 테스트 작성

---

## 2. 아키텍처 진화

### 2-1. 모놀리식 → 멀티모듈 모놀리식

#### 문제

전통적인 모놀리식 구조에서는 코드가 뒤섞여 한 부분을 수정하면 다른 부분이 영향을 받았습니다. 도메인이 늘어날수록 의존성이 폭발적으로 증가했습니다.

#### 방안 검토

| 방안 | 장점 | 단점 | 선택 |
|---|---|---|---|
| 전통 모놀리식 유지 | 단순한 배포 | 코드 결합도 높음, 수정 영향 범위 예측 불가 | ❌ |
| MSA 전환 | 독립 배포, 기술 자율성 | 분산 트랜잭션, 인프라 복잡도, 2개월 프로젝트에 과다 | ❌ |
| 멀티모듈 모놀리식 | 모듈 경계 명확, 단일 배포 유지, MSA 전환 용이 | 모듈 간 규칙 준수 필요 | ✅ |

#### 적용

MSA의 설계 원칙(모듈 경계, Public API 노출)을 지키면서도 단일 배포의 이점을 유지하는 **멀티모듈 모놀리식**을 선택했습니다.

```
CalFit/
├── core/
│   ├── core-api/        # REST Controller, Facade (오케스트레이션)
│   ├── core-enum/       # 공유 Enum
│   └── domain-*/        # 13개 도메인 모듈 (billing, payment, user, ...)
├── storage/
│   └── db-core/         # JPA Entity, Repository
├── support/             # security, error, event, logging, monitoring, email
└── clients/             # 외부 API 연동 (Google Calendar)
```

각 도메인 모듈은 **명확한 경계**를 가지며, Public API(Reader 인터페이스)로만 외부에 노출합니다. 내부 서비스나 Repository를 직접 호출하면 모놀리식과 다를 게 없기 때문입니다.

---

### 2-2. 4계층 + Facade 패턴 도입

#### 문제

멀티모듈로 분리했지만, 두 가지 경계 침범이 발생했습니다.

1. **Service가 타 도메인 Repository를 직접 import** → 모듈 경계 무의미
2. **여러 도메인을 조합하는 로직이 Controller에 누적** → Controller 비대화, 테스트 어려움

```java
// ❌ BillingService가 UserRepository를 직접 참조 → 경계 침범
public class BillingService {
    private final UserRepository userRepository;  // 다른 도메인의 내부 구현에 의존
}
```

#### 방안 검토

| 문제 | 방안 | 선택 이유 |
|---|---|---|
| 타 도메인 접근 | **Reader 인터페이스 계약** | 동기 조회를 유지하면서 구현을 숨김 |
| Controller 비대화 | **Facade 패턴** | 오케스트레이션 전담, Controller는 라우팅만 |

#### 적용: 4계층 구조

도메인 모듈 내부에 Reader(읽기 계약)를 인터페이스로 정의하고, 구현체는 infra 계층에 격리합니다.

```
domain-billing/
├── api/v1/              # Request/Response DTO
├── domain/              # 비즈니스 로직 + Reader 인터페이스 정의
│   ├── BillingService.java
│   ├── MemberReader.java     # ← 인터페이스 (계약)
│   └── BillingInfo.java
└── infra/               # Reader 구현체 (Repository 접근)
    └── MemberReaderImpl.java  # ← 구현 (db-core 의존)
```

```java
// ✅ BillingService는 인터페이스에만 의존 → 구현체 교체 가능, 경계 유지
public class BillingService {
    private final MemberReader memberReader;  // 인터페이스만 알면 됨
}
```

#### 적용: Facade 오케스트레이션

Facade가 여러 도메인 서비스를 조합하고, Controller는 라우팅만 담당합니다. 예를 들어 `ScheduleFacade`는 `MemberReader`(멤버 검증) + `MeetingRoomService`(회의실 조회) + `ScheduleService`(일정 생성)를 조합하여 하나의 유즈케이스를 완성합니다.

#### 결과: 의존성 방향 Before / After

```
[Before] Service 간 양방향 의존 + Controller 비대화
┌──────────┐    ┌──────────┐    ┌──────────┐
│ Billing  │───→│  User    │───→│  Report  │
│ Service  │←───│ Service  │←───│ Service  │
└──────────┘    └──────────┘    └──────────┘
     ↕ 양방향 의존, 변경 시 연쇄 영향

[After] Reader 인터페이스 + Facade로 단방향 의존
┌──────────────────────────────────────────┐
│            core-api (Facade)             │ ← 오케스트레이션만
├──────────────────────────────────────────┤
│ domain-billing │ domain-report │ domain-*│ ← Reader 인터페이스 정의
├──────────────────────────────────────────┤
│           infra (ReaderImpl)             │ ← 구현체
├──────────────────────────────────────────┤
│         storage/db-core (JPA)            │ ← 영속성
└──────────────────────────────────────────┘
     ↓ 단방향 의존, 변경 영향 격리
```

---

## 3. Claude Code + TDD 개발 프로세스

### 왜 AI + TDD인가

Claude Code는 빠른 코드 생성이 가능하지만, 무조건 신뢰하면 버그가 누적됩니다. Kent Beck이 제안한 **Augmented Coding** — TDD 사이클(Red-Green-Refactor)로 개발자가 주도권을 유지하며 AI를 통제하는 접근을 채택했습니다.

```
 ┌─────────────────────────────────────────────────────┐
 │                                                     │
 │   1. 요구사항 정의 (GitHub Issue)                     │
 │          ↓                                          │
 │   2. 실패하는 테스트 먼저 작성 (Red) ← 개발자가 직접   │
 │          ↓                                          │
 │   3. Claude Code로 구현 코드 생성 (Green) ← AI 담당   │
 │          ↓                                          │
 │   4. 테스트 실행 → 실패 시 3번으로 (자동 피드백 루프)   │
 │          ↓                                          │
 │   5. 테스트 통과 → 리팩토링 (Refactor) ← 개발자 판단   │
 │          ↓                                          │
 │   6. PR → CI 자동 검증 (JaCoCo + SonarCloud)         │
 │                                                     │
 └─────────────────────────────────────────────────────┘
```

### 구체 사례: 멀티테넌시 모듈 구현

| 단계 | 수행 주체 | 내용 |
|---|---|---|
| **Red** | 개발자 | 테스트 3건 작성: TenantId 자동 발급 / 테넌트 간 데이터 격리 / 헤더 없는 요청 거부 |
| **Green** | Claude Code | TenantContext(ThreadLocal) + TenantInterceptor + BaseEntity tenantId 필드 생성 |
| **피드백** | 테스트 실행 | 2건 실패 — 예외 타입 불일치 + TenantContext.clear() 누락 → AI에 피드백 후 재생성 |
| **Refactor** | 개발자 | 테스트 전체 통과 확인 → 코드 구조 정리 → PR |

이 패턴을 담당 도메인 전체에 반복 적용했습니다.

### 테스트 인프라 설계

TDD를 팀 전체에 적용하려면 **"테스트 작성이 쉬워야"** 합니다. 이를 위해 테스트 인프라를 먼저 구축했습니다.

```java
// 모든 통합 테스트의 설정을 한 줄로 통일
@CalfitApiTest
class POST_specs {  // HTTP 메서드 기반 네이밍 컨벤션

    @Test
    void 올바른_토큰이면_200_OK와_멤버_목록을_반환한다(
            @Autowired MemberFixture fixture) {
        // Arrange — Fixture가 회원가입 + 워크스페이스 + 멤버 초대를 캡슐화
        WorkspaceContext ctx = fixture.setupWorkspace();

        // Act
        ApiResponse<MemberResponse[]> response =
                fixture.getMembers(ctx.hrToken(), ctx.workspaceId());

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    }
}
```

| 테스트 인프라 요소 | 역할 |
|---|---|
| `@CalfitApiTest` | `@SpringBootTest` + Fixture 자동 주입 + 랜덤 포트를 메타 어노테이션으로 통일 |
| `BaseFixture` | GET/POST/PUT/DELETE HTTP 클라이언트를 래핑, 모든 Fixture의 기반 |
| `{Domain}Fixture` | 도메인별 테스트 시나리오 캡슐화 (UserFixture, MemberFixture, BillingFixture 등) |
| `{HTTP_METHOD}_specs.java` | API 엔드포인트별 테스트 파일 네이밍 컨벤션 |
| REST Docs 연동 | 통합 테스트 기반으로 Mock 데이터를 주입하여 API 문서 자동 생성 |
| 멀티테넌트 격리 | `TenantContext` 기반 테넌트 간 데이터 격리를 모든 테스트에서 검증 |
| 비동기 이벤트 | Awaitility로 도메인 이벤트의 eventual consistency 검증 |

### 이 프로세스가 만든 차이

| 항목 | 효과 |
|---|---|
| **속도** | 테스트 작성 → AI 구현 생성 반복으로 담당 6개 도메인 모듈 완성 |
| **안정성** | AI가 생성한 코드도 반드시 테스트를 통과해야 머지 — 검증 없는 코드 차단 |
| **유지보수** | 테스트가 곧 명세서 — 6개월 후에도 의도를 파악 가능 |
| **팀 기여** | 테스트 인프라 구축으로 팀원 전체가 동일한 TDD 프로세스를 따를 수 있게 함 |

핵심 인사이트: Claude Code는 **"코드를 대신 짜주는 도구"가 아니라 "TDD Green 단계를 가속하는 도구"**로 활용했습니다. 개발자가 Red(테스트)와 Refactor(설계 판단)를 주도하고, AI는 구현 생성만 담당하기 때문에 속도와 품질을 동시에 확보할 수 있었습니다.

---

## 4. CI/CD 파이프라인

### 듀얼 파이프라인 설계

EC2 + GitHub Actions로 빠른 배포 환경을 구축한 뒤, **컨테이너 오케스트레이션 경험을 위해** EKS + Jenkins 파이프라인을 추가 구축했습니다. 현재 서비스는 모듈러 모놀리식이고 트래픽 규모도 크지 않아 EKS가 필수는 아니지만, 두 방식을 직접 비교하며 **각 배포 전략의 장단점과 적합한 상황**을 체득하는 것이 목적이었습니다.

| 항목 | EC2 배포 | EKS 배포 |
|---|---|---|
| 목적 | 빠른 MVP 배포 | 컨테이너 오케스트레이션 학습 및 비교 |
| CI/CD 도구 | GitHub Actions | Jenkins |
| 트리거 | main 브랜치 push | Jenkins 수동 빌드 / Webhook |
| 이미지 레지스트리 | DockerHub | AWS ECR |
| 배포 방식 | docker-compose | kubectl Rolling Update |
| 외부 접근 | EC2 Public IP | ALB DNS |

### EC2 배포 흐름 (GitHub Actions)

```
 개발자 → main push → GitHub Actions
                          ↓
                    Gradle Build (-x test)
                          ↓
                    Docker Buildx → DockerHub push (:latest)
                          ↓
                    SCP docker-compose.yml → EC2
                          ↓
                    SSH: docker pull → docker-compose up -d
                          ↓
                    EC2에서 BE + MySQL 실행
```

### EKS 배포 흐름 (Jenkins)

```
 개발자 → Jenkins 빌드 실행
              ↓
        Checkout → Gradle bootJar
              ↓
        Docker Build (ECR 태그 :BUILD_NUMBER + :latest)
              ↓
        ECR Push
              ↓
        kubectl set image (Rolling Update)
              ↓
        EKS Pod 재시작 → ALB 헬스체크 통과
              ↓
        배포 완료
```

### AWS 인프라 구성

```
                    ┌─── AWS Cloud ───────────────────────────┐
                    │                                         │
   사용자 ──→ CloudFront ──→ S3 (FE 정적 파일)                │
                    │                                         │
   사용자 ──→ ALB ──→ EKS Cluster                             │
                    │   ├── Pod (Spring Boot + MySQL Sidecar) │
                    │   └── PVC (EBS 10Gi)                    │
                    │                                         │
              EC2 ──→ docker-compose (BE + MySQL)             │
                    │                                         │
              Jenkins EC2 ──→ ECR ──→ EKS 배포                │
                    └─────────────────────────────────────────┘
```

| AWS 서비스 | 용도 |
|---|---|
| S3 + CloudFront | FE 정적 파일 호스팅 + CDN |
| EC2 | BE 서버 (docker-compose) / Jenkins 서버 |
| EKS | Kubernetes 클러스터 (t3.medium × 2) |
| ECR | Docker 이미지 레지스트리 |
| ALB | EKS Ingress 로드밸런서 |
| EBS | MySQL 데이터 영구 저장 (PersistentVolume) |
