# CHSBackPOCV1

채널 백엔드 PRD를 Spring Boot 코드로 옮기는 첫 번째 POC입니다. 참조 기능은 가상의 채널 요청 접수·조회입니다. `RECEIVED`는 로컬 DB에 접수 기록이 저장됐다는 뜻입니다.

## 시작하기

JDK 17, Spring Boot 4.1.2, Gradle Wrapper 8.14.7를 사용합니다. 첫 빌드에는 Gradle 배포본과 Maven 의존성을 받을 수 있는 네트워크가 필요합니다. SQLite는 JDBC 의존성으로 포함되며 DB 서버 설치는 필요하지 않습니다. Spring Framework 7.0.10은 Boot 4.1.2의 BOM으로 관리하며, Boot 4의 Web MVC·RestClient 모듈과 Jackson 3를 사용합니다.

```sh
./gradlew test
./gradlew bootRun
```

Gradle 다운로드 중 `PKIX path building failed`가 발생하면 사내 승인 CA를 신뢰하는 JDK 또는 신뢰 저장소를 사용합니다. 기존 빌드 결과는 재다운로드 없이 실행할 수 있습니다.

```sh
java -jar build/libs/CHSBackPOCV1-0.1.0-SNAPSHOT.jar
```

기본 주소는 `http://127.0.0.1:8080`입니다. 포트가 사용 중이면 `SERVER_PORT=8081 ./gradlew bootRun`으로 실행합니다. SQLite는 실행 디렉터리에 `chsback-poc.sqlite3`를 만들며, `SQLITE_DB_PATH`로 파일 경로를 지정할 수 있습니다. 별도 경로를 지정할 때는 상위 디렉터리를 먼저 만듭니다. DB 파일은 Git에서 제외됩니다.

## 예제 REST 계약

| 예제 PRD ID | API | 요청 | 성공 | 검수 기준 |
| --- | --- | --- | --- | --- |
| REQ-001 | `POST /api/v1/channel-requests` | `{"subject":"POC 채널 요청"}` | `201 Created`, `Location` 및 응답 DTO | 공백 또는 120자 초과 제목은 `400`. 정상 접수는 DB에 저장 |
| REQ-002 | `GET /api/v1/channel-requests/{id}` | 경로의 UUID | `200 OK` 및 응답 DTO | 존재하지 않는 UUID는 `404`, UUID 형식 오류는 `400` |

```sh
curl -i http://127.0.0.1:8080/api/v1/channel-requests \
  -H 'Content-Type: application/json' \
  --data '{"subject":"POC 채널 요청"}'
# POST 응답의 Location 헤더 값을 대입해 조회합니다.
location="/api/v1/channel-requests/여기에-응답-UUID"
curl -i "http://127.0.0.1:8080${location}"
```

성공 응답은 `id`(UUID), `subject`, `status`, `createdAt`(ISO 8601 UTC) 필드를 사용합니다. 오류는 Spring MVC의 Problem Details 형식을 사용합니다.

## 코드 구조

```text
JSON → Controller → Service → DBIO → SQLite
       요청·응답 DTO    업무 규칙       SQL·행 매핑
```

| 위치 | 역할 |
| --- | --- |
| `controller/` | 경로, HTTP 상태, 요청 객체 검증, JSON 응답 |
| `dto/` | 외부 공개 요청·응답 계약. Java record와 Bean Validation 사용 |
| `service/` | 업무 규칙, 트랜잭션 경계와 DTO 매핑 |
| `dbio/` | JdbcClient 바인딩 SQL과 DB 행 매핑. 외부 API DTO와 DB 행을 분리 |
| `src/main/resources/db/sqlite/schema.sql` | 로컬 최초 실행용 스키마 |
| `ChannelRequestApiTest` | API → Service → DBIO → SQLite 통합 검증 |

## 다음 프로젝트에 전달할 기준

1. PRDMaker에서 확정한 채널 백엔드 PRD의 문서 버전과 요구사항 ID를 코드 생성 입력으로 넘깁니다. 각 API에는 메서드·경로, DTO 필드와 필수 여부, 정상·오류 응답, 검수 기준을 명시합니다.
2. PRD에 정의된 업무 정책을 Service에 배치하고, 계정계 연계가 있으면 전문 ID·버전, 필드 매핑, 오류 코드와 타임아웃 정책을 확인한 뒤 전용 연계 모듈을 추가합니다. 누락된 계약은 확인 질문으로 남깁니다.
3. 테이블 변경은 키·제약, 저장 필드, 트랜잭션 범위와 함께 검토합니다. 현재 `CREATE TABLE IF NOT EXISTS`는 새 파일을 준비하는 용도입니다. 스키마가 진화하면 버전별 마이그레이션을 도입합니다.
4. 코드 생성 결과에 `요구사항 ID → API → DBIO/테이블 → 테스트` 매핑을 남기고 `./gradlew test`를 실행합니다. 주석에는 계약이나 데이터 변환의 이유처럼 코드만으로 알기 어려운 결정을 적습니다.

이 버전은 채널 백엔드 참조 프로젝트입니다. PRD에서 코드 자동 생성, 운영 EIMS 연결과 실제 거래 로직은 후속 단계입니다.

## 계정계 HTTP와 EIMS 샘플

채널 API의 요청·응답은 `dto/`의 `ProductInquiryRequest`와 `ProductInquiryResponse`입니다. 계정계 HTTP 전문은 `integration/core/vo/`의 `CoreProductInquiryVo`로 정의합니다. 서비스에서 채널 DTO와 내부 조회 결과를 매핑하고, 외부 클라이언트에서 전문 VO를 구성·검증합니다.

```text
채널 JSON → Controller DTO → Service → CoreProductClient → 전문 VO → 계정계 HTTP
```

| 항목 | 샘플 계약 |
| --- | --- |
| 전문 | `CHS-CORE-001` v1.0, 상품 기본정보 조회 |
| 채널 API | `POST /api/v1/core/product-inquiries` |
| 계정계 HTTP | `POST /core/v1/product-inquiries`, JSON |
| 헤더 | `messageId`, `version`, 요청마다 새로 발급한 UUID `requestId`. 계정계가 응답에서 그대로 반환 |
| 본문 | 요청 `productCode`, 성공 응답 `productCode`·`productName` |
| 결과 | 전문 `0000` → 채널 200, `1001` → 404. HTTP 실패·계약 불일치 → 502, 타임아웃 → 504 |

[샘플 EIMS 등록 초안](docs/eims/CHS-CORE-001-v1.0.json)에 필드·결과 코드·요청 및 응답 예시가 있습니다. 전문 ID, 형식과 코드값은 POC에서 정한 예시입니다. 실제 연동에서는 승인된 EIMS 계약과 버전을 소스 생성 입력으로 받고, 전문 VO·클라이언트·계약 테스트를 같은 변경에서 반영합니다. 현재 채널 런타임은 계정계에 직접 요청합니다.

`CORE_BASE_URL` 기본값은 `http://127.0.0.1:8091`입니다. `CORE_CONNECT_TIMEOUT_MS`와 `CORE_READ_TIMEOUT_MS`로 타임아웃을 조정합니다. 요청은 계약상 조회이며 SQLite에 별도로 저장하지 않습니다.

계정계 샘플 서버 없이 검증하려면 `./gradlew test --tests '*CoreProductInquiryApiTest'`를 실행합니다. 테스트가 로컬 HTTP 스텁을 띄우고 실제 전송 JSON, 응답 DTO 매핑과 오류 처리를 확인합니다. 별도 계정계 서버를 띄웠다면 `CORE_BASE_URL`을 지정해 채널을 실행하고 아래 요청을 보냅니다.

```sh
curl -i http://127.0.0.1:8080/api/v1/core/product-inquiries \
  -H 'Content-Type: application/json' \
  --data '{"productCode":"SAMPLE-DEPOSIT"}'
```

## MariaDB로 넘어갈 때

MariaDB JDBC 드라이버와 별도 프로필을 추가하고 URL·계정은 환경변수로 주입합니다. MariaDB용 DDL과 마이그레이션을 분리한 뒤 시간·문자 타입, 키·제약, 동시성, 트랜잭션 동작을 확인합니다. SQLite의 단일 커넥션 설정은 로컬 POC에 한정합니다. Controller 계약은 API 테스트로 유지하고 DBIO와 매핑은 실제 MariaDB에서도 검증합니다.
