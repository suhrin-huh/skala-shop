-- SKALA-FUND 초기 스키마 (MySQL 8, utf8mb4)
--
-- prod 는 spring.jpa.hibernate.ddl-auto=validate 이므로 이 스키마가
-- com.skala.fund.domain 의 엔티티와 한 컬럼이라도 어긋나면 애플리케이션이 기동하지 못한다.
-- 엔티티를 바꾸면 이 파일을 고치지 말고 새 버전(V3, V4 ...)을 추가한다.
--
-- 명명 규칙은 Hibernate 기본(CamelCaseToUnderscoresNamingStrategy)을 따른다.
--   reservedPoint -> reserved_point, searchTitle -> search_title, deliveryStatus -> delivery_status
-- LocalDateTime -> datetime(6), LocalDate -> date, Long -> bigint, Integer -> int
-- created_at / updated_at 은 BaseTimeEntity(@CreatedDate / @LastModifiedDate) 가 채우므로
-- DB 기본값을 두지 않고 nullable 로 둔다. DB 가 값을 채우면 감사 필드가 이중으로 관리된다.

-- ---------------------------------------------------------------------------
-- customer : 회원. point 는 보유 포인트, reserved_point 는 PLEDGED 상태로 묶인 포인트.
--            사용 가능 포인트 = point - reserved_point (비정규화 컬럼, 애플리케이션이 락으로 동기화)
-- ---------------------------------------------------------------------------
CREATE TABLE customer (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    email          VARCHAR(100) NOT NULL,
    nickname       VARCHAR(50)  NOT NULL,
    password       VARCHAR(255) NOT NULL,
    point          BIGINT       NOT NULL,
    reserved_point BIGINT       NOT NULL,
    created_at     DATETIME(6)  NULL,
    updated_at     DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_customer_email UNIQUE (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- category : 카테고리 20종. 초기 데이터는 V2 가 넣는다.
--            display_order 가 화면 노출 순서이며 CategoryCatalog 의 선언 순서와 같다.
-- ---------------------------------------------------------------------------
CREATE TABLE category (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    name          VARCHAR(50) NOT NULL,
    display_order INT         NOT NULL,
    created_at    DATETIME(6) NULL,
    updated_at    DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_category_name UNIQUE (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- project : 펀딩 프로젝트.
--   search_title    공백 제거 + 소문자 변환된 검색 전용 사본 (LIKE 검색용)
--   current_amount  모금액 비정규화. 후원/취소 시 즉시 갱신
--   pledge_count    후원 건수 비정규화
--   deleted_at      Soft Delete. NULL 이면 살아있는 프로젝트
-- ---------------------------------------------------------------------------
CREATE TABLE project (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    creator_id     BIGINT       NOT NULL,
    category_id    BIGINT       NOT NULL,
    title          VARCHAR(100) NOT NULL,
    search_title   VARCHAR(100) NOT NULL,
    description    TEXT         NOT NULL,
    main_image     VARCHAR(500) NOT NULL,
    target_amount  BIGINT       NOT NULL,
    current_amount BIGINT       NOT NULL,
    pledge_count   BIGINT       NOT NULL,
    start_date     DATE         NOT NULL,
    end_date       DATE         NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    deleted_at     DATETIME(6)  NULL,
    created_at     DATETIME(6)  NULL,
    updated_at     DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_project_creator  FOREIGN KEY (creator_id)  REFERENCES customer (id),
    CONSTRAINT fk_project_category FOREIGN KEY (category_id) REFERENCES category (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 검색: WHERE search_title LIKE '%키워드%' 는 인덱스를 못 타지만,
-- 접두 검색과 커버링 스캔에서는 효과가 있어 유지한다.
CREATE INDEX idx_project_search_title ON project (search_title);

-- 목록 조회: 살아있는(deleted_at IS NULL) 프로젝트를 status 로 거르는 게 기본 패턴이다.
CREATE INDEX idx_project_deleted_at_status ON project (deleted_at, status);

-- 상태 전이 배치: 마감일 지난 ONGOING 을 훑는다.
CREATE INDEX idx_project_status_end_date ON project (status, end_date);

-- 내가 만든 프로젝트 목록
CREATE INDEX idx_project_creator_deleted_at ON project (creator_id, deleted_at);

-- 카테고리별 목록
CREATE INDEX idx_project_category_deleted_at ON project (category_id, deleted_at);

-- ---------------------------------------------------------------------------
-- pledge : 후원 내역.
--   status          PLEDGED / CANCELLED / CONFIRMED / FAILED
--   delivery_status CONFIRMED 건에만 값이 있다. 그 외에는 NULL 이므로 nullable 이어야 한다.
-- ---------------------------------------------------------------------------
CREATE TABLE pledge (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    customer_id     BIGINT      NOT NULL,
    project_id      BIGINT      NOT NULL,
    amount          BIGINT      NOT NULL,
    status          VARCHAR(20) NOT NULL,
    delivery_status VARCHAR(20) NULL,
    created_at      DATETIME(6) NULL,
    updated_at      DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_pledge_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
    CONSTRAINT fk_pledge_project  FOREIGN KEY (project_id)  REFERENCES project (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 정산 배치와 비정규화 대조 쿼리:
--   SUM(amount) WHERE project_id = ? AND status = 'PLEDGED'  <->  project.current_amount
CREATE INDEX idx_pledge_project_id_status ON pledge (project_id, status);

-- 사용 가능 포인트 대조 쿼리:
--   SUM(amount) WHERE customer_id = ? AND status = 'PLEDGED' <->  customer.reserved_point
CREATE INDEX idx_pledge_customer_id_status ON pledge (customer_id, status);

-- ---------------------------------------------------------------------------
-- project_like : 찜. 한 사람이 같은 프로젝트를 두 번 찜할 수 없다.
--                DB 유니크가 없으면 동시 요청에서 중복 행이 생긴다.
-- ---------------------------------------------------------------------------
CREATE TABLE project_like (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    customer_id BIGINT      NOT NULL,
    project_id  BIGINT      NOT NULL,
    created_at  DATETIME(6) NULL,
    updated_at  DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_project_like_customer_project UNIQUE (customer_id, project_id),
    CONSTRAINT fk_project_like_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
    CONSTRAINT fk_project_like_project  FOREIGN KEY (project_id)  REFERENCES project (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- refresh_token : 리프레시 토큰. token 은 유니크.
--   VARCHAR(500) * 4바이트(utf8mb4) = 2000바이트로 InnoDB 유니크 인덱스 한도(3072B) 안이다.
-- ---------------------------------------------------------------------------
CREATE TABLE refresh_token (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    customer_id BIGINT       NOT NULL,
    token       VARCHAR(500) NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,
    created_at  DATETIME(6)  NULL,
    updated_at  DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_token UNIQUE (token),
    CONSTRAINT fk_refresh_token_customer FOREIGN KEY (customer_id) REFERENCES customer (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_refresh_token_customer_id ON refresh_token (customer_id);
