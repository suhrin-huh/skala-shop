package com.skala.fund.config;

import com.skala.fund.common.util.CategoryCatalog;
import com.skala.fund.domain.Category;
import com.skala.fund.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.List;

/**
 * dev 프로파일 부팅 시 카테고리 20종을 넣는다.
 * prod 는 ddl-auto: validate 이므로 Flyway 마이그레이션(V2)이 같은 역할을 한다.
 *
 * data.sql 대신 Runner 를 쓰는 이유: 하네스 시드가 카테고리를 지웠다 다시 넣기 때문에
 * 카탈로그 정의를 Java 한 곳(CategoryCatalog)에만 두는 편이 어긋날 여지가 적다.
 */
@Slf4j
@Profile("dev")
@Configuration
@RequiredArgsConstructor
public class CategoryInitializer {

    @Bean
    public ApplicationRunner initCategories(CategoryRepository categoryRepository) {
        return args -> {
            if (categoryRepository.count() > 0) {
                return;
            }
            List<Category> categories = new ArrayList<>();
            for (int i = 0; i < CategoryCatalog.NAMES.size(); i++) {
                categories.add(Category.builder()
                        .name(CategoryCatalog.NAMES.get(i))
                        .displayOrder(i + 1)
                        .build());
            }
            categoryRepository.saveAll(categories);
            log.info("카테고리 초기 데이터 {}건 생성 완료", categories.size());
        };
    }
}
