package com.skala.fund.common.util;

import java.util.List;

/**
 * 확정된 카테고리 20종. 순서가 곧 displayOrder 다.
 *
 * 주의: `웹툰·만화`는 완성된 작품, `웹툰 리소스`는 웹툰 제작용 배경·소재 파일이다.
 * 이름이 비슷하지만 별개 카테고리이므로 통합하지 않는다.
 */
public final class CategoryCatalog {

    public static final List<String> NAMES = List.of(
            "디자인 문구", "푸드", "출판", "영화·비디오", "보드게임·TRPG",
            "캐릭터·굿즈", "향수·뷰티", "디자인·일러스트", "공연", "홈·리빙",
            "의류", "문화·예술", "웹툰·만화", "테크·가전", "잡화",
            "사진", "웹툰 리소스", "반려동물", "주얼리", "음악"
    );

    private CategoryCatalog() {
    }
}
