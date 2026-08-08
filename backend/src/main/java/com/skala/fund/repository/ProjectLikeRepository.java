package com.skala.fund.repository;

import com.skala.fund.domain.Customer;
import com.skala.fund.domain.Project;
import com.skala.fund.domain.ProjectLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectLikeRepository extends JpaRepository<ProjectLike, Long> {

    Optional<ProjectLike> findByCustomerAndProject(Customer customer, Project project);

    boolean existsByCustomerAndProject(Customer customer, Project project);

    /**
     * 목록에 실린 프로젝트 중 이 회원이 찜한 것들의 ID.
     * 카드마다 exists 를 날리면 N+1 이 되므로 한 번에 받아 Set 으로 판정한다.
     */
    @Query("SELECT l.project.id FROM ProjectLike l WHERE l.customer.id = :customerId AND l.project.id IN :projectIds")
    List<Long> findLikedProjectIds(@Param("customerId") Long customerId,
                                   @Param("projectIds") List<Long> projectIds);

    /** 찜 목록에서는 삭제된 프로젝트를 제외한다. */
    @Query(value = "SELECT l FROM ProjectLike l JOIN FETCH l.project p JOIN FETCH p.creator JOIN FETCH p.category "
            + "WHERE l.customer = :customer AND p.deletedAt IS NULL ORDER BY l.createdAt DESC",
            countQuery = "SELECT COUNT(l) FROM ProjectLike l WHERE l.customer = :customer AND l.project.deletedAt IS NULL")
    Page<ProjectLike> findMyLikes(@Param("customer") Customer customer, Pageable pageable);
}
