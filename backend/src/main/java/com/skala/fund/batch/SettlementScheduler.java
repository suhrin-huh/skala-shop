package com.skala.fund.batch;

import com.skala.fund.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 매일 00:10 에 마감된 프로젝트를 정산한다.
 * 돈이 움직이는 지점이므로 대상 건수와 성공/실패를 반드시 로깅한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementScheduler {

    private final SettlementService settlementService;

    @Scheduled(cron = "0 10 0 * * *")
    public int run() {
        List<Long> targetIds = settlementService.findSettlementTargetIds();
        log.info("[배치] 정산 시작 - 대상 {}건", targetIds.size());

        int succeeded = 0;
        int failed = 0;

        // 프로젝트 단위로 settlementService 를 호출해 트랜잭션을 분리한다.
        // 한 건이 터져도 나머지 프로젝트 정산은 롤백되지 않아야 한다.
        for (Long projectId : targetIds) {
            try {
                settlementService.settleSingleProject(projectId);
                succeeded++;
            } catch (Exception e) {
                failed++;
                log.error("[배치] 정산 실패 - projectId={}", projectId, e);
            }
        }

        log.info("[배치] 정산 종료 - 대상 {}건, 성공 {}건, 실패 {}건", targetIds.size(), succeeded, failed);
        return succeeded;
    }
}
