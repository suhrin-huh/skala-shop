package com.skala.fund.domain;

import com.skala.fund.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "pledge")
public class Pledge extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PledgeStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DeliveryStatus deliveryStatus;

    @Builder
    public Pledge(Customer customer, Project project, Long amount, PledgeStatus status, DeliveryStatus deliveryStatus) {
        this.customer = customer;
        this.project = project;
        this.amount = amount;
        this.status = status != null ? status : PledgeStatus.PLEDGED;
        this.deliveryStatus = deliveryStatus;
    }

    public void cancel() {
        this.status = PledgeStatus.CANCELLED;
    }

    public void confirm() {
        this.status = PledgeStatus.CONFIRMED;
        this.deliveryStatus = DeliveryStatus.ORDER_COMPLETED;
    }

    public void markFailed() {
        this.status = PledgeStatus.FAILED;
    }

    public void updateDeliveryStatus(DeliveryStatus newStatus) {
        this.deliveryStatus = newStatus;
    }
}
