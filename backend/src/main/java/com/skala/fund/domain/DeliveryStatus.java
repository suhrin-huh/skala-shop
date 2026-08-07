package com.skala.fund.domain;

/**
 * CONFIRMED 상태 후원에만 값이 존재한다. 별도 Reward 엔티티 없이 이 상태로 리워드 이행을 판단한다.
 * 선언 순서가 곧 진행 단계이며 역행할 수 없다.
 */
public enum DeliveryStatus {
    ORDER_COMPLETED,
    SHIPPING,
    DELIVERED;

    /** target 이 현재 단계보다 뒤(또는 같은 단계)인지 확인한다. 같은 단계로의 변경은 멱등 처리한다. */
    public boolean canTransitionTo(DeliveryStatus target) {
        return target != null && target.ordinal() >= this.ordinal();
    }
}
