# 동시성 처리

## 메뉴 수정·삭제

수정이 삭제 전 값을 저장하면서 `deleted=true`를 덮어쓰는 문제가 있었습니다. 수정·삭제 모두 `findWithLockByIdAndDeletedFalse`의 `PESSIMISTIC_WRITE` 잠금을 사용하도록 고쳤습니다. 잠금은 트랜잭션 종료까지 유지합니다.

| 먼저 잠금을 얻은 작업 | 결과 |
| --- | --- |
| 삭제 → 수정 | 삭제 성공, 수정 404, 삭제 상태 유지 |
| 수정 → 삭제 | 수정·삭제 성공, 수정한 값과 삭제 상태 유지 |

READ COMMITTED에서 대기 중인 `SELECT FOR UPDATE`는 변경된 행의 조회 조건을 다시 검사합니다. [PostgreSQL 공식 문서](https://www.postgresql.org/docs/18/transaction-iso.html#XACT-READ-COMMITTED)
