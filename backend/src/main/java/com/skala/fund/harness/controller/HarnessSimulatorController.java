package com.skala.fund.harness.controller;

import com.skala.fund.batch.ProjectStatusScheduler;
import com.skala.fund.batch.SettlementScheduler;
import com.skala.fund.common.response.ApiResponse;
import com.skala.fund.harness.service.HarnessSeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 개발/테스트 전용 하네스 시뮬레이터.
 * 배치는 원래 매일 00:05 / 00:10 에만 돌기 때문에, 이 트리거 없이는 하루를 기다려야 검증할 수 있다.
 *
 * prod 프로파일에서는 절대 활성화되지 않는다.
 */
@Slf4j
@Profile({"dev", "local"})
@RestController
@RequestMapping("/api/harness")
@RequiredArgsConstructor
public class HarnessSimulatorController {

    private final ProjectStatusScheduler projectStatusScheduler;
    private final SettlementScheduler settlementScheduler;
    private final HarnessSeedService harnessSeedService;

    @PostMapping("/batch/run-status-transition")
    public ApiResponse<String> runStatusTransitionBatch() {
        int count = projectStatusScheduler.run();
        return ApiResponse.success("상태 전이 배치 실행 완료. 처리 건수: " + count);
    }

    @PostMapping("/batch/run-settlement")
    public ApiResponse<String> runSettlementBatch() {
        int count = settlementScheduler.run();
        return ApiResponse.success("정산 배치 실행 완료. 처리 건수: " + count);
    }

    @PostMapping("/seed")
    public ApiResponse<String> seedData() {
        return ApiResponse.success(harnessSeedService.seed());
    }
}
