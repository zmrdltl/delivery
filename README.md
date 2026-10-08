# Delivery

Java 21과 Spring Boot로 만든 배달 주문 REST API입니다. 하나의 서버에서 `OWNER`와 `CUSTOMER`의 권한을 구분합니다. 사장은 가게와 메뉴를 등록하고 주문을 처리하며, 손님은 메뉴를 조회하고 주문·결제·취소합니다.

## 설계 문서

| 문서 | 내용 |
| --- | --- |
| [ERD](docs/erd.md) | 엔티티 6개, PK·FK, 관계의 수 |
| [테이블 명세](docs/table-spec.md) | 컬럼, 타입, 제약, 주문 시점 정보 보존 |
| [API 명세](docs/api-spec.md) | API 17개, 권한, 요청·응답, 오류, 상태 전이 |
| [인프라 구성](docs/infrastructure.md) | 내 PC의 Postman → Spring Boot → PostgreSQL 흐름 |
| [동시성 설계·검증](docs/concurrency.md) | 메뉴 수정·삭제와 주문·결제의 잠금, DB 테스트 실행 방법 |

문서는 현재 구현을 기준으로 작성했습니다. 과제의 필수 기능, 도전 기능, 권장 설계 문서, 추가 동시성 개선을 구분합니다.

## 기능과 과제 범위

| 범위 | 구현 |
| --- | --- |
| 필수 | 회원가입·로그인, BCrypt 비밀번호 저장, JWT 인증, 역할과 소유권 검사 |
| 필수 | 메뉴 등록·목록·단건·수정·Soft Delete, 서버에서 주문 금액 계산 |
| 필수 | 역할별 주문 목록, CARD 모의 결제, 수락·배달 완료·미결제 취소 |
| 필수 구조 | 공통 생성·수정 시각, LAZY 연관관계, 문자열 enum, DTO 응답, 요청 검증 |
| 도전 | 본인 주문·가게 주문의 단건 조회, 본인 결제 내역 조회 |
| 도전 | 메뉴 목록 페이징·최신순 정렬, 주문 생성 후 5분 이내 취소 |
| 도전 | 결제 후 취소와 결제 상태 변경, 사장의 주문 거절 |
| 도전 | 공통 오류 형식과 401·403 구분, Service 성공·실패 단위 테스트 |
| 도전 | 한 주문에 여러 메뉴, `Store`와 `OrderItem` 분리 |
| 권장 문서 | ERD, 테이블 명세, API 명세, 로컬 인프라 구성 |
| 추가 개선 | 메뉴 수정·삭제 동시 요청에서 삭제 표시가 덮어써지는 문제 방지 |

외부 결제사 승인·환불과 서버 배포는 구현 범위에 포함하지 않습니다. 결제는 DB에 CARD 결제 기록을 저장하는 모의 처리입니다.

## 실행 환경

- Java 21
- Gradle Wrapper: 저장소의 `gradlew` 사용
- Spring Boot 4.1.1, Spring Security, Spring Data JPA, JJWT 0.13.0
- PostgreSQL 18: 로컬 Docker 컨테이너 사용
- API 주소: `http://localhost:8080`

### 1. PostgreSQL 준비

기존 `delivery-db` 컨테이너가 있으면 Docker를 실행하고 해당 컨테이너를 시작합니다.

```bash
docker start delivery-db
```

새 환경에서 컨테이너를 만들 때는 사용할 DB 비밀번호를 화면에 표시하지 않고 입력합니다. 이미 컨테이너가 있으면 아래 생성 명령은 실행하지 않습니다.

```bash
read -r -s SPRING_DATASOURCE_PASSWORD
export SPRING_DATASOURCE_PASSWORD

docker run --name delivery-db \
    -e POSTGRES_USER=delivery \
    -e POSTGRES_PASSWORD="$SPRING_DATASOURCE_PASSWORD" \
    -e POSTGRES_DB=delivery \
    -p 127.0.0.1:5432:5432 \
    -d postgres:18
```

컨테이너에 다른 포트를 연결했다면 애플리케이션의 접속 URL도 같은 포트로 설정합니다. 기존 컨테이너를 삭제하거나 DB 데이터를 초기화할 필요는 없습니다.

### 2. 비밀 설정 준비

공통 `application.yml`에는 DB 주소·사용자명·JWT 만료 시간과 일반 설정이 있습니다. 실제 DB 비밀번호와 JWT 키는 Git에 올리지 않습니다.

새로 clone한 환경에서만 `src/main/resources/application-local.yml`을 직접 만듭니다. 아래 두 자리표시자를 자신의 값으로 바꿉니다. 이미 로컬 설정이 있으면 덮어쓰지 않습니다.

```yaml
spring:
  datasource:
    password: "YOUR_LOCAL_DB_PASSWORD"

jwt:
  secret: "YOUR_BASE64_JWT_SECRET"
```

JWT 키는 최소 32바이트의 무작위 데이터를 Base64로 인코딩한 값입니다. 새 개발 환경의 키 생성 예:

```bash
openssl rand -base64 32
```

로컬 설정 파일은 `.gitignore`에 포함되어 있습니다. 서버는 HS256으로 토큰에 서명하며 기본 만료 시간은 3,600,000ms입니다.

### 3. 애플리케이션 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

IntelliJ에서는 Project SDK·Gradle JVM을 Java 21로 설정하고 `DeliveryApplication`의 활성 프로필에 `local`을 지정합니다.

파일 대신 환경변수를 사용하려면 `SPRING_DATASOURCE_PASSWORD`와 `JWT_SECRET`을 설정하고 `./gradlew bootRun`으로 실행할 수 있습니다. DB 주소·사용자명은 필요에 따라 `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`으로 덮어씁니다.

### 기존 DB의 결제 상태 컬럼

새 DB는 Hibernate가 엔티티를 읽어 테이블을 생성합니다. 이전 버전의 DB에 `payments.status` 컬럼이 없고 기존 결제 기록이 있을 때만 [일회성 SQL](scripts/sql/add_payment_status.sql)을 앱 실행 전에 적용합니다.

```bash
docker exec -i delivery-db psql -U delivery -d delivery < scripts/sql/add_payment_status.sql
```

이미 `status` 컬럼이 있는 DB나 새 DB에는 이 SQL을 재실행하지 않습니다. `ddl-auto: update`가 기존 enum의 CHECK 제약까지 모두 변경해 주는 것은 아니므로, enum 값을 추가한 기존 DB는 제약도 확인합니다.

## API 확인 순서

1. `POST /api/users`로 `OWNER`와 `CUSTOMER`를 각각 가입합니다.
2. `POST /api/users/login`으로 로그인하여 각 토큰을 보관합니다.
3. 사장 토큰으로 `POST /api/stores` → `POST /api/menus`를 호출하고 반환된 ID를 보관합니다.
4. 손님 토큰으로 `POST /api/orders`를 호출합니다. 여러 메뉴는 같은 가게에 속해야 합니다.
5. 손님이 결제한 뒤 사장이 수락·배달 완료합니다. 별도 주문으로 취소·거절을 확인합니다.
6. 다른 사용자 토큰으로 접근해 403이 나오는지 확인합니다.

메뉴 GET은 공개 API입니다. 보호된 API에는 `Authorization: Bearer <토큰>`을 보냅니다. 가게 목록 API는 현재 없으므로 생성 응답의 `id`를 사용합니다. 요청 예시는 [API 명세](docs/api-spec.md)에 있습니다.

## 테스트

기본 테스트는 DB·Docker·`local` 프로필 없이 실행합니다.

```bash
./gradlew test
```

Service 성공·실패, 권한·소유권·상태 전이, 금액 보존·오버플로, 5분 경계, DTO 검증과 JSON 정수 필드의 소수 거절을 검증합니다. Mockito 기반 테스트가 실제 DB 잠금을 검증하는 것은 아닙니다.

실제 PostgreSQL의 메뉴 수정·삭제 경쟁은 별도 태스크로 실행합니다.

```bash
export DB_TEST_URL='jdbc:postgresql://localhost:5432/delivery'
export DB_TEST_USERNAME='delivery'
read -r -s DB_TEST_PASSWORD
export DB_TEST_PASSWORD
./gradlew integrationTest
```

이 테스트는 매번 무작위 이름의 전용 스키마를 만들고 종료 시 그 스키마만 삭제합니다. 기존 `public` 스키마의 사용자·메뉴·주문은 건드리지 않습니다. 스키마 생성 권한이 있는 개발·테스트용 DB를 사용합니다. 자세한 검증 순서와 실행 조건은 [동시성 문서](docs/concurrency.md)를 확인합니다.
