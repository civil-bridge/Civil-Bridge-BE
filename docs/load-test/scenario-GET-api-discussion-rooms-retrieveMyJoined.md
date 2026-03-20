# 부하테스트 시나리오 — 내가 참여한 논의방 목록 조회 (GET /api/discussion-rooms/retrieveMyJoined)

> 작성일: 2026-03-14
> analyze-bottleneck 분석 결과 기반으로 자동 작성.
> `[직접 채울 것]` 항목은 비즈니스 맥락 판단이 필요하므로 직접 작성한다.

---

## Step 1. 대상 선정

### 선정 이유 (내 판단 — 비즈니스 맥락)

[직접 채울 것: 이 API를 선정한 이유 — 호출 빈도, 기능 중요도 등]

### 예상 병목 (코드 분석 결과)

| 분석 항목 | 내용 | 코드 위치 |
|-----------|------|-----------|
| 실행 쿼리 수 | DB 쿼리 3개 고정: ①discussion_rooms INNER JOIN members WHERE user_id=? ORDER BY m.created_at DESC (페이징) ②COUNT(*) FROM discussion_rooms JOIN members WHERE user_id=? (Page 총 건수) ③members GROUP BY room_id WHERE room_id IN (...) | DiscussionRoomService.java:236, 249 |
| N+1 가능성 | DB 레벨 N+1 없음. 단, Redis HGETALL이 루프에서 방 건수(최대 size)만큼 직렬 호출됨 — 파이프라이닝/mget 없이 방별 1회씩 | DiscussionRoomService.java:251-263, DiscussionRoomCacheRepositoryImpl.java:336-354 |
| 인덱스 활용 여부 | JOIN 쿼리 `WHERE m.user_id = ? ORDER BY m.created_at DESC` 시, members 테이블에 `UNIQUE(user_id, room_id)`만 존재. user_id WHERE는 커버되나 **`created_at` 컬럼이 인덱스에 없어 filesort 발생**. `(user_id, created_at DESC)` 복합 인덱스 누락 | V1__init.sql:51, MemberJpaRepository.java:36 |
| 동시성 이슈 가능성 | `@Transactional(readOnly = true)` — 락 없음. leaveRoom의 PESSIMISTIC_WRITE 락과 이 엔드포인트는 충돌하지 않음 | DiscussionRoomService.java:230 |
| 캐시 적용 여부 | 방 정보(`room:{roomId}` Hash)는 방별 캐시 적용. 단 **목록 레벨에서는 캐시 미사용** — `CacheRepositoryImpl.retrieveJoinedRoomsByPage()`가 구현되어 있으나 서비스에서 호출되지 않고 항상 DB `findRoomsByUserId` 실행 | DiscussionRoomService.java:236, DiscussionRoomCacheRepositoryImpl.java:157-203 |

---

## Step 2. 가설 수립

**가설:**

```
GET /api/discussion-rooms/retrieveMyJoined에 동시 사용자 100명이 요청하면,
members 테이블에 (user_id, created_at DESC) 복합 인덱스가 없어 filesort가 발생하고,
동시에 Redis HGETALL이 캐시 미스 시 최대 size(기본 15)건 직렬 호출되기 때문에
응답시간이 Vuser 증가에 비례해 선형 증가하고, P95 응답시간이 1초를 초과할 것이다.
```

**가설의 근거:**

1. **filesort 발생 (주요 원인)**: `MemberJpaRepository.java:36`의 JPQL
   `SELECT dr FROM DiscussionRoomEntity dr, MemberEntity m WHERE dr.id = m.roomId AND m.userId = :userId ORDER BY m.createdAt DESC`는
   V1__init.sql:51의 `UNIQUE(user_id, room_id)` 인덱스로 user_id 필터는 처리하지만,
   `created_at`이 인덱스에 없으므로 MySQL은 JOIN 후 결과를 메모리에서 정렬(filesort)한다.
   사용자가 참여한 방 수가 많아질수록 정렬 비용이 선형 증가하며,
   동시 요청이 많을 경우 MySQL I/O 및 sort buffer 경합이 발생한다.
   COUNT(*) JOIN 쿼리도 동일 조건이므로 인덱스 미활용 영향을 받는다.

2. **Redis 직렬 round trip (보조 원인)**: `DiscussionRoomService.java:251-263`의
   `rooms.stream().map(room -> cacheRepository.getCachedRoomOnly(room.getId())...)`는
   방별로 `HGETALL`을 순차 호출한다. 캐시 미스 시 `cacheRoomInfo()` (HSET + EXPIRE)까지
   추가되어 size=15 기준 최악 45 Redis round trips가 직렬 발생한다.
   캐시 히트 비율이 낮은 초반(워밍업 전) 또는 TTL 만료 직후 구간에서 응답시간 스파이크가 예상된다.

**가설이 맞다면 개선 방향:**

| 원인 | 개선 방향 |
|------|-----------|
| filesort (인덱스 누락) | `CREATE INDEX idx_members_user_created ON members (user_id, created_at DESC);` 추가하여 WHERE + ORDER BY를 단일 인덱스 스캔으로 처리 |
| Redis 직렬 루프 | Redis Pipeline 또는 Lua 스크립트로 일괄 HGETALL 처리. 미스분만 DB 조회 후 일괄 캐싱 |
| 목록 레벨 캐시 미사용 | `CacheRepositoryImpl.retrieveJoinedRoomsByPage()`를 `retrieveJoinedRooms()` 서비스에서 실제로 호출하도록 연결 |

**가설이 틀렸을 때 추적 포인트:**

| 실제 증상 | 의심 원인 | 확인 방법 |
|-----------|-----------|-----------|
| TPS가 일정 수준에서 안 올라감 | 커넥션 풀 소진 또는 CPU 한계 | `SHOW STATUS LIKE 'Threads%'`, `top` |
| 응답시간이 Vuser에 비례해서 증가 | filesort 또는 느린 쿼리 | Slow Query Log, `EXPLAIN SELECT dr.room_id FROM discussion_rooms dr INNER JOIN members m ON dr.id = m.room_id WHERE m.user_id=1 ORDER BY m.created_at DESC` |
| CPU 100%인데 TPS가 낮음 | WAS 레벨 병목 (Redis 직렬 round trip) | EC2 `top`, Redis `MONITOR` 명령 |
| 에러율 급증 | 커넥션 타임아웃 또는 메모리 부족 | 에러 로그, `dmesg` |
| Redis 응답이 느림 | Redis single-thread 처리 한계 | `redis-cli INFO stats`, `latency history` |

---

## Step 3. 테스트 설계

### 3-1. 테스트 조건

| 항목 | 설정값 | 이유 |
|------|--------|------|
| Vuser | 10 → 50 → 100 → 200 | 단계적으로 올려서 TPS가 꺾이는 지점 확인 |
| Duration | 각 단계 5분 | 워밍업 이후 수렴 구간에서 안정적인 평균값 확보 |
| Ramp-up | 10초마다 10명 추가 | 스파이크 없이 점진적 부하 |
| 목표 TPS | [직접 채울 것] | |

### 3-2. 테스트 데이터

| 테이블 | 데이터 수 | 이유 |
|--------|-----------|------|
| users | 200명 | Vuser 최대 200명 기준, 각 사용자가 별도 계정으로 요청 |
| discussion_rooms | 1,000개 | 사용자당 평균 5개 참여 가정 시 200명 × 5 = 1,000개 |
| members | 1,000건 (사용자당 5건 균등 분산) | filesort 비용 측정을 위해 사용자당 참여 방 수를 일정하게 고정 |

> **데이터 설계 의도**: 인덱스 미적용 시 filesort는 사용자당 참여 방 수에 비례한다.
> 참여 방 수를 고정(5건)하면 쿼리 비용이 일정하므로, TPS 저하가 filesort보다 동시성 문제인지 판단할 수 있다.
> 참여 방 수를 50건으로 늘린 별도 시나리오를 추가하면 filesort 영향도를 격리 측정할 수 있다.

**삽입 스크립트:**

```sql
-- 1. 테스트 유저 200명 생성
INSERT INTO users (login_id, login_pw, name, nickname, email, phone_number, role)
SELECT
    CONCAT('loadtest_user_', seq),
    '$2a$10$hashedpassword',
    CONCAT('테스트유저', seq),
    CONCAT('nickname_', seq),
    CONCAT('loadtest', seq, '@test.com'),
    CONCAT('010-0000-', LPAD(seq, 4, '0')),
    'USER'
FROM (
    SELECT seq FROM (
        SELECT (@seq := @seq + 1) AS seq
        FROM information_schema.columns, (SELECT @seq := 0) AS init
        LIMIT 200
    ) AS numbers
) AS t;

-- 2. 논의방 1,000개 생성
INSERT INTO discussion_rooms (title, description, city, district, access_level)
SELECT
    CONCAT('부하테스트 논의방 ', seq),
    '부하테스트용 논의방입니다.',
    '수원시',
    '영통구',
    'PUBLIC'
FROM (
    SELECT seq FROM (
        SELECT (@seq2 := @seq2 + 1) AS seq
        FROM information_schema.columns, (SELECT @seq2 := 0) AS init
        LIMIT 1000
    ) AS numbers
) AS t;

-- 3. 각 유저를 5개 논의방에 참여시키기 (user_id 1~200, room_id 각 5개씩 분산)
-- 예: user 1 → room 1~5, user 2 → room 6~10, ...
INSERT INTO members (user_id, room_id)
SELECT
    u.user_id,
    r.room_id
FROM (SELECT user_id, ROW_NUMBER() OVER (ORDER BY user_id) AS rn FROM users WHERE login_id LIKE 'loadtest_user_%') u
JOIN (SELECT room_id, ROW_NUMBER() OVER (ORDER BY room_id) AS rn FROM discussion_rooms) r
  ON r.rn BETWEEN (u.rn - 1) * 5 + 1 AND u.rn * 5;
```

### 3-3. 인증 처리

```groovy
// nGrinder 스크립트 (Groovy)
// 사전 준비: 200명 유저의 Access Token을 미리 발급 후 배열에 저장

import static net.grinder.script.Grinder.grinder
import static org.junit.Assert.assertThat
import static org.hamcrest.Matchers.is
import net.grinder.plugin.http.HTTPRequest
import net.grinder.plugin.http.HTTPPluginControl

// 테스트 유저별 토큰 목록 (사전 발급 필요)
def tokens = [
    "Bearer eyJhbGciOiJIUzI1NiJ9...", // loadtest_user_1 토큰
    // ... 200개
]

@Test
public void test() {
    def token = tokens[grinder.threadNumber % tokens.size()]
    def page = 1
    def size = 15

    HTTPResponse response = request.GET(
        "http://[EC2_IP]:8080/api/discussion-rooms/retrieveMyJoined?page=${page}&size=${size}",
        [
            "Authorization": token,
            "Content-Type" : "application/json"
        ]
    )
    assertThat(response.statusCode, is(200))
}
```

### 3-4. 사전 설정 체크리스트

- [ ] 테스트 데이터 DB에 삽입 완료 (users 200, discussion_rooms 1000, members 1000)
- [ ] 각 테스트 유저(loadtest_user_1 ~ 200) Access Token 발급 완료 (만료 시간 1h 이상 확인)
- [ ] Slow Query Log 활성화
  ```sql
  SET GLOBAL slow_query_log = 'ON';
  SET GLOBAL long_query_time = 1;
  SHOW VARIABLES LIKE 'slow_query_log_file';
  ```
- [ ] 인덱스 미적용 상태 확인 (Before 측정용)
  ```sql
  SHOW INDEX FROM members;
  EXPLAIN SELECT dr.room_id FROM discussion_rooms dr INNER JOIN members m ON dr.id = m.room_id WHERE m.user_id = 1 ORDER BY m.created_at DESC LIMIT 15;
  -- Extra 컬럼에 "Using filesort" 확인
  ```
- [ ] EC2에서 `htop` 띄워두기
- [ ] Redis `redis-cli MONITOR` 띄워두기 (Redis 호출 패턴 확인)
- [ ] 워밍업 테스트 1회 실행 (Vuser 10, 1분)

---

## Step 4. 측정 시나리오 (Before / After)

### Before: 인덱스 없는 상태

현재 상태 그대로 부하테스트 실행.
`EXPLAIN` 결과에서 `Using filesort` 확인 후 TPS, P95 응답시간 기록.

### After: 인덱스 추가 상태

```sql
-- 인덱스 추가
CREATE INDEX idx_members_user_created ON members (user_id, created_at DESC);

-- EXPLAIN으로 filesort 제거 확인
EXPLAIN SELECT dr.room_id FROM discussion_rooms dr INNER JOIN members m ON dr.id = m.room_id WHERE m.user_id = 1 ORDER BY m.created_at DESC LIMIT 15;
-- members 테이블의 Extra 컬럼에 "Using filesort" 사라지고 "Using index" 확인
```

인덱스 추가 후 동일 조건으로 재측정. Before 대비 TPS와 P95 개선 폭 비교.

### 측정 결과 기록 양식

| Vuser | Before TPS | Before P95 | After TPS | After P95 | 개선율 |
|-------|-----------|------------|-----------|-----------|--------|
| 10 | | | | | |
| 50 | | | | | |
| 100 | | | | | |
| 200 | | | | | |
