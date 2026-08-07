package com.skala.fund.batch;

import com.skala.fund.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매일 00:05 에 시작일이 도래한 프로젝트를 ONGOING 으로 전이시킨다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectStatusScheduler {

    private final SettlementService settlementService;

    @Scheduled(cron = "0 5 0 * * *")
    public int run() {
        log.info("[배치] 프로젝트 상태 전이 시작");
        return settlementService.runStatusTransition();
    }
}
