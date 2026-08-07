package com.skala.fund.domain;

import com.skala.fund.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "project")
public class Project extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Customer creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 100)
    private String searchTitle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 500)
    private String mainImage;

    @Column(nullable = false)
    private Long targetAmount;

    @Column(nullable = false)
    private Long currentAmount;

    @Column(nullable = false)
    private Long pledgeCount;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status;

    private LocalDateTime deletedAt;

    @Builder
    public Project(Customer creator, Category category, String title, String description,
                   String mainImage, Long targetAmount, LocalDate startDate, LocalDate endDate,
                   ProjectStatus status) {
        this.creator = creator;
        this.category = category;
        this.title = title;
        this.searchTitle = title != null ? title.replaceAll("\\s+", "").toLowerCase() : "";
        this.description = description;
        this.mainImage = mainImage;
        this.targetAmount = targetAmount;
        this.currentAmount = 0L;
        this.pledgeCount = 0L;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status != null ? status : ProjectStatus.SCHEDULED;
    }

    public void update(Category category, String title, String description, String mainImage,
                       Long targetAmount, LocalDate startDate, LocalDate endDate) {
        this.category = category;
        this.title = title;
        this.searchTitle = title != null ? title.replaceAll("\\s+", "").toLowerCase() : "";
        this.description = description;
        this.mainImage = mainImage;
        this.targetAmount = targetAmount;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void changeStatus(ProjectStatus newStatus) {
        this.status = newStatus;
    }

    public void addPledgeAmount(long amount) {
        this.currentAmount += amount;
        this.pledgeCount += 1;
    }

    public void removePledgeAmount(long amount) {
        this.currentAmount = Math.max(0L, this.currentAmount - amount);
        this.pledgeCount = Math.max(0L, this.pledgeCount - 1);
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
