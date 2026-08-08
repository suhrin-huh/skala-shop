package com.skala.fund.config;

import com.skala.fund.harness.service.HarnessSeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * dev 프로파일 부팅 시 카테고리·계정·프로젝트 목업 데이터를 자동으로 채운다.
 * H2 가 인메모리(create-drop)라 재기동할 때마다 비므로, 다른 사람이 저장소를 그대로
 * 내려받아 서버만 켜도 곧바로 데이터가 채워진 화면을 볼 수 있도록 매번 다시 시딩한다.
 * 실제 시딩 로직은 하네스 검증에서 쓰던 HarnessSeedService 를 그대로 재사용한다.
 */
@Profile("dev")
@Configuration
@RequiredArgsConstructor
public class MockDataInitializer {

    private final HarnessSeedService harnessSeedService;

    @Bean
    public ApplicationRunner seedMockData() {
        return args -> harnessSeedService.seed();
    }
}
