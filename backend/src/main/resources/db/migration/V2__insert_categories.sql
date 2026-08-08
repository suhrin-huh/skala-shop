-- 확정 카테고리 20종.
--
-- 정본은 com.skala.fund.common.util.CategoryCatalog#NAMES 이며, 아래 순서는 그 선언 순서를
-- 그대로 옮긴 것이다. display_order = 선언 순서(1-base).
-- 카탈로그가 바뀌면 이 파일을 고치지 말고 새 마이그레이션을 추가한다.
--
-- 주의: `웹툰·만화`(13번, 완성된 작품)와 `웹툰 리소스`(17번, 웹툰 제작용 배경·소재 파일)는
--       이름이 비슷하지만 별개 카테고리다. 하나로 합치지 않는다.
--
-- 가운뎃점(·)이 들어간 이름이 많으므로 이 파일은 반드시 UTF-8 로 저장한다.
-- (application-prod.yml 의 spring.flyway.encoding: UTF-8 참조)

INSERT INTO category (name, display_order, created_at, updated_at) VALUES
    ('디자인 문구',      1,  NOW(6), NOW(6)),
    ('푸드',             2,  NOW(6), NOW(6)),
    ('출판',             3,  NOW(6), NOW(6)),
    ('영화·비디오',      4,  NOW(6), NOW(6)),
    ('보드게임·TRPG',    5,  NOW(6), NOW(6)),
    ('캐릭터·굿즈',      6,  NOW(6), NOW(6)),
    ('향수·뷰티',        7,  NOW(6), NOW(6)),
    ('디자인·일러스트',  8,  NOW(6), NOW(6)),
    ('공연',             9,  NOW(6), NOW(6)),
    ('홈·리빙',         10,  NOW(6), NOW(6)),
    ('의류',            11,  NOW(6), NOW(6)),
    ('문화·예술',       12,  NOW(6), NOW(6)),
    ('웹툰·만화',       13,  NOW(6), NOW(6)),
    ('테크·가전',       14,  NOW(6), NOW(6)),
    ('잡화',            15,  NOW(6), NOW(6)),
    ('사진',            16,  NOW(6), NOW(6)),
    ('웹툰 리소스',     17,  NOW(6), NOW(6)),
    ('반려동물',        18,  NOW(6), NOW(6)),
    ('주얼리',          19,  NOW(6), NOW(6)),
    ('음악',            20,  NOW(6), NOW(6));
