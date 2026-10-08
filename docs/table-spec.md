# 테이블 명세

PostgreSQL과 현재 JPA 엔티티를 기준으로 작성했습니다. `@GeneratedValue(IDENTITY)` PK를 사용하고, enum은 `EnumType.STRING`으로 저장합니다. 자바의 `camelCase` 필드는 DB에서 `snake_case` 컬럼으로 매핑됩니다.

## 공통 감사 컬럼

아래 두 컬럼은 모든 테이블에 실제로 존재합니다. 각 테이블 명세에서는 반복을 생략합니다.

| 컬럼 | DB 타입 | 제약 | 의미 |
| --- | --- | --- | --- |
| `created_at` | TIMESTAMP | NOT NULL, JPA 갱신 제외 | 생성 시각 |
| `updated_at` | TIMESTAMP | NOT NULL | 마지막 수정 시각 |

`BaseEntity`의 `@MappedSuperclass`, `AuditingEntityListener`, 메인 클래스의 `@EnableJpaAuditing`으로 기록합니다. `LocalDateTime`을 사용하므로 시간대 오프셋을 저장하지 않습니다. 취소 기한 계산도 서버의 동일한 로컬 시간 기준입니다.

## users

| 컬럼 | DB 타입 | 제약 | 의미 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, IDENTITY | 내부 회원 번호 |
| `login_id` | VARCHAR(20) | UNIQUE, NOT NULL | 로그인 아이디 |
| `password` | VARCHAR(255) | NOT NULL | BCrypt 해시 |
| `role` | VARCHAR(255) | NOT NULL, enum CHECK | OWNER / CUSTOMER |

아이디 4~20자, 비밀번호 8자 이상·UTF-8 72바이트 이하는 회원가입 요청 검증입니다. 실명·닉네임 필드는 현재 없습니다. 응답 DTO에는 비밀번호를 넣지 않습니다.

## stores

| 컬럼 | DB 타입 | 제약 | 의미 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, IDENTITY | 가게 번호 |
| `owner_id` | BIGINT | FK → users(id), NOT NULL | 사장 |
| `name` | VARCHAR(255) | NOT NULL | 가게 이름 |

사장 한 명이 여러 가게를 만들 수 있으며 `owner_id`에는 UNIQUE를 두지 않습니다.

## menus

| 컬럼 | DB 타입 | 제약 | 의미 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, IDENTITY | 메뉴 번호 |
| `store_id` | BIGINT | FK → stores(id), NOT NULL | 소속 가게 |
| `name` | VARCHAR(255) | NOT NULL | 메뉴 이름 |
| `price` | BIGINT | NOT NULL | 가격, 원 단위 정수 |
| `description` | TEXT | NULL 허용 | 선택 설명 |
| `deleted` | BOOLEAN | NOT NULL | Soft Delete 표시 |

가격 양수 검사는 요청 DTO의 `@Positive`로 수행합니다. `deleted`의 자바 초기값은 `false`이며 별도 DB DEFAULT를 선언하지 않습니다. 수정과 삭제는 활성 메뉴를 행 잠금으로 조회한 같은 트랜잭션에서 처리합니다.

## orders

| 컬럼 | DB 타입 | 제약 | 의미 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, IDENTITY | 주문 번호 |
| `customer_id` | BIGINT | FK → users(id), NOT NULL | 주문 손님 |
| `store_id` | BIGINT | FK → stores(id), NOT NULL | 대상 가게 |
| `address` | VARCHAR(255) | NOT NULL | 배달 주소 |
| `total_price` | BIGINT | NOT NULL | 주문 항목 금액 합계 |
| `status` | VARCHAR(255) | NOT NULL, enum CHECK | ORDERED / PAID / ACCEPTED / DELIVERED / CANCELED / REJECTED |

주문 금액은 서버가 활성 메뉴의 가격 × 수량으로 계산합니다. 클라이언트가 총액을 지정하는 요청 필드는 없습니다. 곱셈·합계가 `long` 범위를 넘으면 저장 전에 400으로 거절합니다.

## order_items

| 컬럼 | DB 타입 | 제약 | 의미 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, IDENTITY | 주문 항목 번호 |
| `order_id` | BIGINT | FK → orders(id), NOT NULL | 부모 주문 |
| `menu_id` | BIGINT | FK → menus(id), NOT NULL | 원본 메뉴 |
| `name` | VARCHAR(255) | NOT NULL | 주문 당시 이름 |
| `unit_price` | BIGINT | NOT NULL | 주문 당시 단가 |
| `quantity` | INTEGER | NOT NULL | 주문 수량 |

수량 양수, 같은 가게 메뉴, 삭제되지 않은 메뉴인지의 검사는 API·Service에서 합니다. 한 주문 안에 같은 메뉴를 여러 항목으로 보내면 별도 항목으로 저장하며 자동 합치지 않습니다.

## payments

| 컬럼 | DB 타입 | 제약 | 의미 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, IDENTITY | 결제 번호 |
| `order_id` | BIGINT | FK → orders(id), UNIQUE, NOT NULL | 결제 대상 주문 |
| `amount` | BIGINT | NOT NULL | 결제 당시 주문 총액 |
| `method` | VARCHAR(255) | NOT NULL, enum CHECK | CARD |
| `status` | VARCHAR(255) | NOT NULL, enum CHECK | PAID / CANCELED |

취소·거절할 때 결제 행을 지우거나 금액을 0으로 만들지 않고 상태만 `CANCELED`로 바꿉니다. 취소된 주문의 재결제는 허용하지 않습니다.

## JPA와 기존 DB 변경

JPA의 NOT NULL·UNIQUE·FK는 DB 제약이고, DTO의 길이·양수·필수 검증은 API 진입 시 검증입니다. 두 종류의 검증을 같은 것으로 보지 않습니다.

개발 설정은 `ddl-auto: update`입니다. 기존 enum CHECK 제약의 변경과 데이터가 있는 테이블에 NOT NULL 컬럼을 추가하는 작업은 자동 갱신만으로 해결되지 않을 수 있습니다. 이전 DB의 결제 상태 추가에는 [일회성 SQL](../scripts/sql/add_payment_status.sql)을 사용합니다. 새 DB에는 실행하지 않습니다.

실제 테이블은 psql의 `\d users`, `\d menus`, `\d orders`, `\d order_items`, `\d payments`로 비교할 수 있습니다.
