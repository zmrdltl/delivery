# API 명세

서버 실행 후 [Swagger UI](http://localhost:8080/swagger-ui.html)에서 요청·응답 스키마와 API별 권한·상태 변경 조건을 확인하고 요청을 보낼 수 있습니다.

1. `POST /api/users`로 가입합니다. role은 `OWNER` 또는 `CUSTOMER`입니다.
2. `POST /api/users/login`을 실행합니다.
3. 응답의 `token` 값을 **Authorize**에 입력합니다. `Bearer ` 접두어는 붙이지 않습니다.

[OpenAPI JSON](http://localhost:8080/v3/api-docs) · [OpenAPI YAML](http://localhost:8080/v3/api-docs.yaml)

명세는 Controller·DTO에서 생성합니다. 실행 방법은 [README](../README.md#실행)를 확인합니다.
