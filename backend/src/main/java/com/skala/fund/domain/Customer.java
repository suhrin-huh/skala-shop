package com.skala.fund.domain;

import com.skala.fund.domain.BaseTimeEntity;
import com.skala.fund.common.exception.CustomException;
import com.skala.fund.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "customer")
public class Customer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false)
    private Long point; // 보유 포인트

    @Column(nullable = false)
    private Long reservedPoint; // PLEDGED 상태 예약 포인트

    @Builder
    public Customer(String email, String nickname, String password, Long point) {
        this.email = email;
        this.nickname = nickname;
        this.password = password;
        this.point = (point != null) ? point : 1_000_000L;
        this.reservedPoint = 0L;
    }

    public long getAvailablePoint() {
        return Math.max(0L, this.point - this.reservedPoint);
    }

    public void reservePoint(long amount) {
        if (getAvailablePoint() < amount) {
            throw new CustomException(ErrorCode.INSUFFICIENT_AVAILABLE_POINT);
        }
        this.reservedPoint += amount;
    }

    public void releaseReservedPoint(long amount) {
        this.reservedPoint = Math.max(0L, this.reservedPoint - amount);
    }

    public void confirmDeduction(long amount) {
        releaseReservedPoint(amount);
        this.point = Math.max(0L, this.point - amount);
    }

    public void addPoint(long amount) {
        this.point += amount;
    }
}
