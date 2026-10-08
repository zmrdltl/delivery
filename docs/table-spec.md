# 테이블 명세

모든 테이블에 아래 시각 컬럼이 있습니다.

| 공통 컬럼 | DB 타입 | 제약 |
| --- | --- | --- |
| `created_at` | TIMESTAMP | NOT NULL, JPA 갱신 제외 |
| `updated_at` | TIMESTAMP | NOT NULL |

## users

| 컬럼 | DB 타입 | 제약 | 의미 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, IDENTITY | 내부 회원 번호 |
| `login_id` | VARCHAR(20) | UNIQUE, NOT NULL | 로그인 아이디 |
| `password` | VARCHAR(255) | NOT NULL | BCrypt 해시 |
| `role` | VARCHAR(255) | NOT NULL, enum CHECK | OWNER / CUSTOMER |

## stores

| 컬럼 | DB 타입 | 제약 | 의미 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, IDENTITY | 가게 번호 |
| `owner_id` | BIGINT | FK → users(id), NOT NULL | 사장 |
| `name` | VARCHAR(255) | NOT NULL | 가게 이름 |

## menus

| 컬럼 | DB 타입 | 제약 | 의미 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, IDENTITY | 메뉴 번호 |
| `store_id` | BIGINT | FK → stores(id), NOT NULL | 소속 가게 |
| `name` | VARCHAR(255) | NOT NULL | 메뉴 이름 |
| `price` | BIGINT | NOT NULL | 가격, 원 단위 정수 |
| `description` | TEXT | NULL 허용 | 선택 설명 |
| `deleted` | BOOLEAN | NOT NULL | Soft Delete 표시 |

## orders

| 컬럼 | DB 타입 | 제약 | 의미 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, IDENTITY | 주문 번호 |
| `customer_id` | BIGINT | FK → users(id), NOT NULL | 주문 손님 |
| `store_id` | BIGINT | FK → stores(id), NOT NULL | 대상 가게 |
| `address` | VARCHAR(255) | NOT NULL | 배달 주소 |
| `total_price` | BIGINT | NOT NULL | 주문 항목 금액 합계 |
| `status` | VARCHAR(255) | NOT NULL, enum CHECK | ORDERED / PAID / ACCEPTED / DELIVERED / CANCELED / REJECTED |

## order_items

| 컬럼 | DB 타입 | 제약 | 의미 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, IDENTITY | 주문 항목 번호 |
| `order_id` | BIGINT | FK → orders(id), NOT NULL | 부모 주문 |
| `menu_id` | BIGINT | FK → menus(id), NOT NULL | 원본 메뉴 |
| `name` | VARCHAR(255) | NOT NULL | 주문 당시 이름 |
| `unit_price` | BIGINT | NOT NULL | 주문 당시 단가 |
| `quantity` | INTEGER | NOT NULL | 주문 수량 |

## payments

| 컬럼 | DB 타입 | 제약 | 의미 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, IDENTITY | 결제 번호 |
| `order_id` | BIGINT | FK → orders(id), NOT NULL | 결제 대상 주문 |
| `amount` | BIGINT | NOT NULL | 결제 당시 주문 총액 |
| `method` | VARCHAR(255) | NOT NULL, enum CHECK | CARD |
| `status` | VARCHAR(255) | NOT NULL, enum CHECK | PAID / CANCELED |
