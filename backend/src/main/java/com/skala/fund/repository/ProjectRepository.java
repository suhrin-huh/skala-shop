package com.skala.fund.repository;

import com.skala.fund.domain.Customer;
import com.skala.fund.domain.Project;
import com.skala.fund.domain.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 프로젝트 조회 쿼리에는 항상 deletedAt 조건을 명시한다.
 * 엔티티에 @SQLRestriction 을 걸면 마이페이지 후원 내역에서 삭제된 프로젝트가 통째로 사라지므로 쓰지 않는다.
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByIdAndDeletedAtIsNull(Long id);

    /**
     * 비정규화 컬럼(currentAmount, pledgeCount)을 갱신하기 전에 프로젝트 행을 잠근다.
     * Customer 락만으로는 서로 다른 회원이 같은 프로젝트에 동시 후원할 때 갱신 유실이 발생한다.
     *
     * 데드락 방지를 위해 잠금 순서는 프로젝트 전역에서 항상 Project -> Customer 다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Project p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Project> findByIdWithLock(@Param("id") Long id);

    /** 정산 배치용. 대상은 상위 쿼리에서 이미 걸러졌으므로 deletedAt 조건을 다시 걸지 않는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Project p WHERE p.id = :id")
    Optional<Project> findByIdWithLockIgnoringDeleted(@Param("id") Long id);

    @Query("SELECT p FROM Project p JOIN FETCH p.creator JOIN FETCH p.category WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Project> findDetailById(@Param("id") Long id);

    /** 인기 프로젝트 5개 — 후원액 합계(비정규화 컬럼) 내림차순. */
    @Query("SELECT p FROM Project p JOIN FETCH p.creator JOIN FETCH p.category "
            + "WHERE p.deletedAt IS NULL ORDER BY p.currentAmount DESC")
    List<Project> findPopular(Pageable pageable);

    /** 홈 배너용 최근 프로젝트 3개 — 등록일 내림차순. */
    @Query("SELECT p FROM Project p JOIN FETCH p.creator JOIN FETCH p.category "
            + "WHERE p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<Project> findRecent(Pageable pageable);

    @Query(value = "SELECT p FROM Project p JOIN FETCH p.creator JOIN FETCH p.category "
            + "WHERE p.creator = :creator AND p.deletedAt IS NULL",
            countQuery = "SELECT COUNT(p) FROM Project p WHERE p.creator = :creator AND p.deletedAt IS NULL")
    Page<Project> findMyProjects(@Param("creator") Customer creator, Pageable pageable);

    /**
     * 목록/검색. 키워드는 애플리케이션에서 이미 공백 제거·소문자 변환된 값이 들어온다.
     * searchTitle 컬럼에 파생값을 저장해두었기 때문에 WHERE 절에서 REPLACE() 를 호출하지 않아도 된다.
     */
    @Query(value = "SELECT p FROM Project p JOIN FETCH p.creator JOIN FETCH p.category "
            + "WHERE p.deletedAt IS NULL "
            + "AND (:categoryId IS NULL OR p.category.id = :categoryId) "
            + "AND (:keyword IS NULL OR p.searchTitle LIKE CONCAT('%', :keyword, '%'))",
            countQuery = "SELECT COUNT(p) FROM Project p WHERE p.deletedAt IS NULL "
                    + "AND (:categoryId IS NULL OR p.category.id = :categoryId) "
                    + "AND (:keyword IS NULL OR p.searchTitle LIKE CONCAT('%', :keyword, '%'))")
    Page<Project> searchProjects(@Param("categoryId") Long categoryId,
                                 @Param("keyword") String keyword,
                                 Pageable pageable);

    /** 최근 본 펀딩 일괄 조회. 없거나 삭제된 ID 는 결과에서 조용히 빠진다. */
    @Query("SELECT p FROM Project p JOIN FETCH p.creator JOIN FETCH p.category "
            + "WHERE p.id IN :ids AND p.deletedAt IS NULL")
    List<Project> findAllByIds(@Param("ids") List<Long> ids);

    List<Project> findByStatusAndStartDateLessThanEqualAndDeletedAtIsNull(ProjectStatus status, LocalDate date);

    List<Project> findByStatusAndEndDateLessThanAndDeletedAtIsNull(ProjectStatus status, LocalDate date);
}
