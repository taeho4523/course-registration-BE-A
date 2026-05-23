# course-registration-BE-A
## 프로젝트 개요
## 기술 스택
## 실행 방법
## 요구사항 해석 및 가정
## 설계 결정과 이유
## 미구현 / 제약사항
## AI 활용 범위
## 데이터 모델 설명
## 테스트 실행 방법
# API 명세

수강 신청 시스템의 REST API 명세입니다. 모든 요청/응답 본문은 `application/json`이며, 시각은 ISO-8601(KST, `+09:00`) 형식을 따릅니다.

## 공통 사항

### 인증

별도 인증/인가 없이 요청 헤더로 사용자를 식별합니다.

```
X-Member-Id: 1
```

해당 헤더가 없거나 존재하지 않는 멤버이면 `401 Unauthorized`를 반환합니다.

### 공통 에러 응답

모든 에러는 아래 형식으로 통일합니다.

```json
{
  "code": "CAPACITY_EXCEEDED",
  "message": "정원이 초과되어 신청할 수 없습니다."
}
```

| HTTP 상태 | 의미 |
|---|---|
| 400 Bad Request | 요청 형식 오류, 잘못된 상태 전이, 취소 가능 기간 초과 |
| 401 Unauthorized | 멤버 식별 실패 |
| 403 Forbidden | 권한 없음 (예: 타인의 강의 수강생 목록 조회) |
| 404 Not Found | 존재하지 않는 리소스 |
| 409 Conflict | 정원 초과, 중복 신청 |

### 페이지네이션

목록 조회는 커서 기반 페이지네이션을 사용합니다.

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `cursor` | long | (없음) | 마지막으로 받은 항목의 id. 미전달 시 처음부터 |
| `size` | int | 20 | 페이지 크기 (최대 100) |

응답 공통 구조:

```json
{
  "content": [ ... ],
  "nextCursor": 42,
  "hasNext": true
}
```

`hasNext`가 `false`이면 마지막 페이지이며 `nextCursor`는 `null`입니다.

---

## 1. 강의 (Course)

### 1.1 강의 등록

```
POST /api/courses
X-Member-Id: 1
```

요청

```json
{
  "title": "Spring Boot 입문",
  "description": "스프링 부트 기초부터 실전까지",
  "price": 50000,
  "capacity": 30,
  "startAt": "2025-06-01T00:00:00+09:00",
  "endAt": "2025-07-31T23:59:59+09:00"
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| title | string | O | 강의명 (최대 100자) |
| description | string | X | 강의 설명 |
| price | int | O | 가격 (원 단위 정수, 0 이상) |
| capacity | int | O | 최대 정원 (1 이상) |
| startAt | datetime | O | 수강 시작일 |
| endAt | datetime | O | 수강 종료일 (startAt 이후) |

응답 `201 Created`

```json
{
  "id": 1,
  "creatorId": 1,
  "title": "Spring Boot 입문",
  "description": "스프링 부트 기초부터 실전까지",
  "price": 50000,
  "capacity": 30,
  "enrolledCount": 0,
  "status": "DRAFT",
  "startAt": "2025-06-01T00:00:00+09:00",
  "endAt": "2025-07-31T23:59:59+09:00"
}
```

등록 직후 상태는 항상 `DRAFT`이며, 헤더의 멤버가 강사(creator)로 지정됩니다.

### 1.2 강의 상태 변경

```
PATCH /api/courses/{id}/status
X-Member-Id: 1
```

요청

```json
{ "status": "OPEN" }
```

허용되는 전이는 `DRAFT → OPEN → CLOSED` 단방향뿐입니다. 역방향이나 단계 건너뛰기(`DRAFT → CLOSED`)는 `400`을 반환합니다. 해당 강의의 강사가 아니면 `403`입니다.

응답 `200 OK` — 변경된 강의 전체를 1.1과 동일한 형태로 반환합니다.

| 상태 | 의미 | 신청 가능 여부 |
|---|---|---|
| DRAFT | 초안 | 불가 |
| OPEN | 모집 중 | 가능 |
| CLOSED | 모집 마감 | 불가 |

### 1.3 강의 목록 조회

```
GET /api/courses?status=OPEN&cursor=&size=20
```

| 쿼리 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| status | string | X | 상태 필터 (DRAFT/OPEN/CLOSED). 미전달 시 전체 |
| cursor | long | X | 페이지네이션 커서 |
| size | int | X | 페이지 크기 |

응답 `200 OK`

```json
{
  "content": [
    {
      "id": 1,
      "title": "Spring Boot 입문",
      "creatorId": 1,
      "price": 50000,
      "capacity": 30,
      "enrolledCount": 12,
      "status": "OPEN"
    }
  ],
  "nextCursor": 1,
  "hasNext": false
}
```

### 1.4 강의 상세 조회

```
GET /api/courses/{id}
```

응답 `200 OK`

```json
{
  "id": 1,
  "creatorId": 1,
  "title": "Spring Boot 입문",
  "description": "스프링 부트 기초부터 실전까지",
  "price": 50000,
  "capacity": 30,
  "enrolledCount": 12,
  "remainingCount": 18,
  "status": "OPEN",
  "startAt": "2025-06-01T00:00:00+09:00",
  "endAt": "2025-07-31T23:59:59+09:00"
}
```

`remainingCount`(잔여 정원) = `capacity - enrolledCount`. 존재하지 않으면 `404`.

### 1.5 강의별 수강생 목록 조회 (강사 전용)

```
GET /api/courses/{id}/enrollments?status=CONFIRMED&cursor=&size=20
X-Member-Id: 1
```

해당 강의의 강사만 조회할 수 있으며, 아니면 `403`. `status`로 신청 상태 필터링이 가능합니다.

응답 `200 OK`

```json
{
  "content": [
    {
      "enrollmentId": 100,
      "memberId": 5,
      "memberName": "김수강",
      "status": "CONFIRMED",
      "enrolledAt": "2025-06-02T10:00:00+09:00",
      "confirmedAt": "2025-06-02T10:05:00+09:00"
    }
  ],
  "nextCursor": 100,
  "hasNext": false
}
```

---

## 2. 수강 신청 (Enrollment)

### 2.1 수강 신청

```
POST /api/courses/{id}/enrollments
X-Member-Id: 5
```

요청 본문 없음. 헤더의 멤버가 신청자입니다.

응답 `201 Created`

```json
{
  "enrollmentId": 100,
  "courseId": 1,
  "memberId": 5,
  "status": "PENDING",
  "enrolledAt": "2025-06-02T10:00:00+09:00"
}
```

**처리 규칙**

- 강의 상태가 `OPEN`이 아니면 `400` (`COURSE_NOT_OPEN`).
- 정원이 가득 차면(`enrolledCount >= capacity`) `409` (`CAPACITY_EXCEEDED`).
- 동일 멤버가 같은 강의에 활성(`PENDING`/`CONFIRMED`) 신청을 이미 보유하면 `409` (`ALREADY_ENROLLED`).
- 정원 확인과 카운터 증가는 Course 행에 비관적 락(`SELECT ... FOR UPDATE`)을 건 단일 트랜잭션 안에서 처리하여, 동시에 마지막 한 자리에 다수가 신청해도 정확히 정원만큼만 성공합니다.

### 2.2 결제 확정

```
POST /api/enrollments/{id}/confirm
X-Member-Id: 5
```

실제 결제 연동 없이 상태만 변경합니다. `PENDING → CONFIRMED` 전이만 허용하며, 이미 `CANCELLED`이거나 `CONFIRMED`이면 `400` (`INVALID_STATE_TRANSITION`).

응답 `200 OK`

```json
{
  "enrollmentId": 100,
  "courseId": 1,
  "memberId": 5,
  "status": "CONFIRMED",
  "enrolledAt": "2025-06-02T10:00:00+09:00",
  "confirmedAt": "2025-06-02T10:05:00+09:00"
}
```

### 2.3 수강 취소

```
POST /api/enrollments/{id}/cancel
X-Member-Id: 5
```

응답 `200 OK`

```json
{
  "enrollmentId": 100,
  "courseId": 1,
  "memberId": 5,
  "status": "CANCELLED",
  "enrolledAt": "2025-06-02T10:00:00+09:00",
  "confirmedAt": "2025-06-02T10:05:00+09:00",
  "cancelledAt": "2025-06-05T09:00:00+09:00"
}
```

**처리 규칙**

- `PENDING`(결제 전)은 기간 제한 없이 취소 가능합니다.
- `CONFIRMED`(확정)는 `confirmedAt` 기준 7일 이내에만 취소 가능하며, 초과 시 `400` (`CANCEL_PERIOD_EXPIRED`).
- 이미 `CANCELLED`이면 `400` (`INVALID_STATE_TRANSITION`).
- 취소 시 Course의 `enrolledCount`를 1 감소시키며(동일 트랜잭션), 대기열에 대기자가 있으면 1번 대기자를 승격합니다(2.5 참조).

### 2.4 내 수강 신청 목록 조회

```
GET /api/enrollments/me?status=CONFIRMED&cursor=&size=20
X-Member-Id: 5
```

헤더 멤버 기준 신청 목록을 반환합니다. `status` 필터 선택.

응답 `200 OK`

```json
{
  "content": [
    {
      "enrollmentId": 100,
      "courseId": 1,
      "courseTitle": "Spring Boot 입문",
      "status": "CONFIRMED",
      "enrolledAt": "2025-06-02T10:00:00+09:00",
      "confirmedAt": "2025-06-02T10:05:00+09:00",
      "cancelledAt": null
    }
  ],
  "nextCursor": 100,
  "hasNext": false
}
```

### 2.5 상태 전이 규칙

허용되는 전이만 정의하고 나머지는 모두 `400`으로 거부합니다.

| 현재 상태 | 가능한 다음 상태 | 트리거 |
|---|---|---|
| PENDING | CONFIRMED | 결제 확정 |
| PENDING | CANCELLED | 취소 |
| CONFIRMED | CANCELLED | 취소 (7일 이내) |
| CANCELLED | (없음) | — |

---

## 3. 대기열 (Waitlist) — 선택 구현

정원이 가득 찬 강의에 대기 등록하고, 자리가 나면 순번대로 승격합니다.

### 3.1 대기 등록

```
POST /api/courses/{id}/waitlist
X-Member-Id: 7
```

응답 `201 Created`

```json
{
  "waitlistId": 200,
  "courseId": 1,
  "memberId": 7,
  "position": 3,
  "status": "WAITING"
}
```

- 강의 정원이 아직 남아 있으면 `400` (`CAPACITY_AVAILABLE`) — 대기가 아니라 바로 신청해야 합니다.
- 이미 대기 중이면 `409` (`ALREADY_WAITING`).
- `position`은 현재 대기열의 마지막 순번 + 1입니다.

### 3.2 내 대기 순번 조회

```
GET /api/courses/{id}/waitlist/me
X-Member-Id: 7
```

응답 `200 OK`

```json
{
  "waitlistId": 200,
  "courseId": 1,
  "position": 3,
  "status": "WAITING"
}
```

### 3.3 승격 동작 (내부 처리)

별도 엔드포인트가 아니라 취소(2.3)로 자리가 났을 때 내부적으로 트리거됩니다.

1. `position`이 가장 앞선 `WAITING` 대기자를 선택합니다.
2. 해당 대기자의 Enrollment를 `PENDING` 상태로 생성하고 결제 기한(예: 24시간)을 부여합니다.
3. 대기 항목 상태를 `PROMOTED`로 변경합니다.
4. 기한 내 결제(2.2)하면 `CONFIRMED`, 미결제 시 스케줄러가 해당 PENDING을 만료 처리하고 다음 대기자를 승격합니다.

> 승격을 곧바로 `CONFIRMED`로 만들지 않는 이유: 결제 없이 수강 확정되는 것은 비즈니스 규칙 위반이므로, 반드시 `PENDING`을 거쳐 결제 기한을 부여합니다.

| 대기 상태 | 의미 |
|---|---|
| WAITING | 대기 중 (순번 보유) |
| PROMOTED | 자리 발생으로 신청(PENDING) 전환됨 |
| EXPIRED | 기한 내 미결제 또는 대기 취소 |
