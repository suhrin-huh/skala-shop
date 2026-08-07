package com.skala.fund.repository;

import com.skala.fund.domain.Customer;
import com.skala.fund.domain.Pledge;
import com.skala.fund.domain.PledgeStatus;
import com.skala.fund.domain.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PledgeRepository extends JpaRepository<Pledge, Long> {

    /**
     * 마이페이지 후원 내역. 삭제된 프로젝트의 후원도 포함해야 하므로 deletedAt 조건을 걸지 않는다.
     * 후원자 입장에서 자기 후원 기록이 사라진 것처럼 보이면 안 된다.
     */
    @Query("SELECT p FROM Pledge p JOIN FETCH p.project pr JOIN FETCH pr.category "
            + "WHERE p.customer = :customer ORDER BY p.createdAt DESC")
    Page<Pledge> findMyPledges(@Param("customer") Customer customer, Pageable pageable);

    List<Pledge> findByProject(Project project);

    List<Pledge> findByProjectAndStatus(Project project, PledgeStatus status);

    /**
     * 회원별 예약액. Customer.reservedPoint 비정규화 값의 정합성을 대조할 때 쓴다(하네스 불변식 I1).
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Pledge p "
            + "WHERE p.customer.id = :customerId AND p.status = com.skala.fund.domain.PledgeStatus.PLEDGED")
    long sumReservedAmountByCustomer(@Param("customerId") Long customerId);

    /**
     * 프로젝트별 모금액. Project.currentAmount 비정규화 값 대조용(하네스 불변식 I3).
     * CANCELLED, FAILED 는 모금액에서 제외한다.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Pledge p WHERE p.project.id = :projectId "
            + "AND p.status IN (com.skala.fund.domain.PledgeStatus.PLEDGED, com.skala.fund.domain.PledgeStatus.CONFIRMED)")
    long sumActiveAmountByProject(@Param("projectId") Long projectId);

    /** 프로젝트별 후원 건수. Project.pledgeCount 대조용(하네스 불변식 I4). */
    @Query("SELECT COUNT(p) FROM Pledge p WHERE p.project.id = :projectId "
            + "AND p.status IN (com.skala.fund.domain.PledgeStatus.PLEDGED, com.skala.fund.domain.PledgeStatus.CONFIRMED)")
    long countActiveByProject(@Param("projectId") Long projectId);

    /** 창작자가 자기 프로젝트의 후원자 목록을 볼 때 사용한다(배송 상태 변경 화면). */
    @Query("SELECT p FROM Pledge p JOIN FETCH p.customer WHERE p.project.id = :projectId "
            + "AND p.status = com.skala.fund.domain.PledgeStatus.CONFIRMED ORDER BY p.createdAt DESC")
    List<Pledge> findConfirmedPledgesByProject(@Param("projectId") Long projectId);
}
