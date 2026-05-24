# API 명세

수강 신청 시스템의 REST API 상세 명세입니다. 모든 요청/응답 본문은 `application/json`이며, 시각은 ISO-8601 형식입니다. 인증은 `X-Member-Id` 헤더로 사용자를 식별합니다.

## 공통

### 인증

```
X-Member-Id: 1
```

### 에러 응답 형식

```json
{
  "code": "CAPACITY_EXCEEDED",
  "message": "정원이 초과되어 신청할 수 없습니다."
}
```

| HTTP 상태 | 대표 코드 | 의미 |
|---|---|---|
| 400 | INVALID_REQUEST, INVALID_COURSE_STATUS_TRANSITION, INVALID_ENROLLMENT_STATUS_TRANSITION, COURSE_NOT_OPEN, CANCEL_PERIOD_EXPIRED, CAPACITY_AVAILABLE | 요청 형식 오류, 잘못된 상태 전이, 취소 기간 초과 등 |
| 401 | UNAUTHORIZED | 멤버 식별 실패 |
| 403 | FORBIDDEN | 권한 없음 |
| 404 | COURSE_NOT_FOUND, ENROLLMENT_NOT_FOUND, WAITLIST_NOT_FOUND, MEMBER_NOT_FOUND | 리소스 없음 |
| 409 | CAPACITY_EXCEEDED, ALREADY_ENROLLED, ALREADY_WAITING | 정원 초과, 중복 신청/대기 |

### 페이지네이션 (커서 기반)

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `cursor` | long | (없음) | 마지막으로 받은 항목의 id. 미전달 시 처음부터 |
| `size` | int | 20 | 페이지 크기 |

응답 공통 구조:

```json
{
  "content": [ ... ],
  "nextCursor": 42,
  "hasNext": true
}
```

목록은 id 내림차순(최신순)으로 정렬됩니다.

---

## 1. 강의 (Course)

### 1.1 강의 등록

```
POST /api/courses
X-Member-Id: 1
```

```json
{
  "title": "Spring Boot 입문",
  "description": "기초부터 실전까지",
  "price": 50000,
  "capacity": 30,
  "startAt": "2025-06-01T00:00:00",
  "endAt": "2025-07-31T23:59:59"
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| title | string | O | 강의명 (최대 100자) |
| description | string | X | 설명 |
| price | int | O | 가격 (0 이상) |
| capacity | int | O | 정원 (1 이상) |
| startAt | datetime | O | 시작일 |
| endAt | datetime | O | 종료일 (시작일 이후) |

응답 `201 Created`

```json
{
  "id": 1,
  "creatorId": 1,
  "title": "Spring Boot 입문",
  "description": "기초부터 실전까지",
  "price": 50000,
  "capacity": 30,
  "enrolledCount": 0,
  "remainingCount": 30,
  "status": "DRAFT",
  "startAt": "2025-06-01T00:00:00",
  "endAt": "2025-07-31T23:59:59"
}
```

등록 직후 상태는 `DRAFT`, 헤더의 멤버가 강사(creator)가 됩니다.

### 1.2 강의 상태 변경

```
PATCH /api/courses/{courseId}/status
X-Member-Id: 1
```

```json
{ "status": "OPEN" }
```

허용 전이: `DRAFT → OPEN → CLOSED` (단방향). 역방향·건너뛰기는 `400`. 강사 본인이 아니면 `403`.

응답 `200 OK` — 변경된 강의 전체 (1.1과 동일 형태).

### 1.3 강의 목록 조회

```
GET /api/courses?status=OPEN&cursor=&size=20
```

| 파라미터 | 필수 | 설명 |
|---|---|---|
| status | X | DRAFT/OPEN/CLOSED 필터. 미전달 시 전체 |
| cursor, size | X | 페이지네이션 |

응답 `200 OK`

```json
{
  "content": [
    { "id": 1, "title": "Spring Boot 입문", "creatorId": 1, "price": 50000, "capacity": 30, "enrolledCount": 12, "status": "OPEN" }
  ],
  "nextCursor": 1,
  "hasNext": false
}
```

### 1.4 강의 상세 조회

```
GET /api/courses/{courseId}
```

응답 `200 OK` — 1.1 형태 + `remainingCount`(잔여 정원 = capacity - enrolledCount). 없으면 `404`.

### 1.5 강의별 수강생 목록 (강사 전용)

```
GET /api/courses/{courseId}/enrollments?status=CONFIRMED&cursor=&size=20
X-Member-Id: 1
```

해당 강의의 강사만 조회 가능, 아니면 `403`.

응답 `200 OK`

```json
{
  "content": [
    { "enrollmentId": 100, "memberId": 5, "memberName": "김수강", "status": "CONFIRMED", "enrolledAt": "...", "confirmedAt": "..." }
  ],
  "nextCursor": 100,
  "hasNext": false
}
```

---

## 2. 수강 신청 (Enrollment)

### 2.1 수강 신청

```
POST /api/courses/{courseId}/enrollments
X-Member-Id: 5
```

요청 본문 없음. 응답 `201 Created`

```json
{
  "enrollmentId": 100,
  "courseId": 1,
  "memberId": 5,
  "status": "PENDING",
  "enrolledAt": "2025-06-02T10:00:00",
  "confirmedAt": null,
  "cancelledAt": null
}
```

**처리 규칙**
- 강의가 `OPEN`이 아니면 `400` (COURSE_NOT_OPEN)
- 정원 초과 시 `409` (CAPACITY_EXCEEDED)
- 활성(PENDING/CONFIRMED) 신청 중복 시 `409` (ALREADY_ENROLLED)
- Course 행 비관적 락(SELECT … FOR UPDATE)으로 정원 확인·증가를 원자적으로 처리

### 2.2 결제 확정

```
POST /api/enrollments/{enrollmentId}/confirm
X-Member-Id: 5
```

`PENDING → CONFIRMED`. 그 외 상태면 `400`. 응답 `200 OK` (confirmedAt 채워짐).

### 2.3 수강 취소

```
POST /api/enrollments/{enrollmentId}/cancel
X-Member-Id: 5
```

- `PENDING`: 기간 제한 없이 취소 가능
- `CONFIRMED`: confirmedAt 기준 7일 이내만 가능, 초과 시 `400` (CANCEL_PERIOD_EXPIRED)
- 취소 시 정원 카운터 감소 + 대기자 있으면 자동 승격

응답 `200 OK` (cancelledAt 채워짐).

### 2.4 내 수강 신청 목록

```
GET /api/enrollments/me?status=CONFIRMED&cursor=&size=20
X-Member-Id: 5
```

응답 `200 OK`

```json
{
  "content": [
    { "enrollmentId": 100, "courseId": 1, "courseTitle": "Spring Boot 입문", "status": "CONFIRMED", "enrolledAt": "...", "confirmedAt": "...", "cancelledAt": null }
  ],
  "nextCursor": 100,
  "hasNext": false
}
```

### 상태 전이 규칙

| 현재 | 가능한 다음 | 트리거 |
|---|---|---|
| PENDING | CONFIRMED | 결제 확정 |
| PENDING | CANCELLED | 취소 |
| CONFIRMED | CANCELLED | 취소 (7일 이내) |
| CANCELLED | (없음) | — |

---

## 3. 대기열 (Waitlist)

### 3.1 대기 등록

```
POST /api/courses/{courseId}/waitlist
X-Member-Id: 7
```

응답 `201 Created`

```json
{ "waitlistId": 200, "courseId": 1, "memberId": 7, "position": 3, "status": "WAITING" }
```

- 정원에 여유가 있으면 `400` (CAPACITY_AVAILABLE) — 바로 신청해야 함
- 이미 대기 중이면 `409` (ALREADY_WAITING)

### 3.2 내 대기 순번 조회

```
GET /api/courses/{courseId}/waitlist/me
X-Member-Id: 7
```

응답 `200 OK`

```json
{ "waitlistId": 200, "courseId": 1, "memberId": 7, "position": 3, "status": "WAITING" }
```

WAITING 상태가 아니면(승격/만료됨) `404` (WAITLIST_NOT_FOUND).

### 3.3 승격 동작 (내부 처리)

취소(2.3)로 자리가 나면 내부적으로 트리거됩니다.

1. position이 가장 앞선 WAITING 대기자를 비관적 락으로 선택
2. Course 카운터를 회수(+1)하고, 대기자 명의로 PENDING 신청 생성
3. 대기 항목을 PROMOTED로 변경하고 승격 시각(promotedAt) 기록
4. 결제 기한(기본 24시간) 내 미결제 시, 스케줄러가 해당 신청을 취소·만료시키고 다음 대기자를 승격

| 대기 상태 | 의미 |
|---|---|
| WAITING | 대기 중 (순번 보유) |
| PROMOTED | 자리 발생으로 PENDING 신청 전환됨 |
| EXPIRED | 대기 취소 또는 결제 기한 만료 |
