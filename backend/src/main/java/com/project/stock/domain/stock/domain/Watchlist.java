package com.project.stock.domain.stock.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 관심 종목 엔티티.
 * 배치 작업에서 분석할 종목 목록을 관리합니다.
 */
@Entity
@Table(
    name = "watchlist",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_watchlist_stock", columnNames = {"stock_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Watchlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "priority", nullable = false)
    private Integer priority; // 우선순위 (낮을수록 먼저 처리)

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    public Watchlist(Stock stock, Boolean isActive, Integer priority) {
        this.stock = stock;
        this.isActive = isActive != null ? isActive : true;
        this.priority = priority != null ? priority : 999;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * 활성화 상태를 변경합니다.
     */
    public void updateActiveStatus(Boolean isActive) {
        this.isActive = isActive;
        this.updatedAt = Instant.now();
    }

    /**
     * 우선순위를 변경합니다.
     */
    public void updatePriority(Integer priority) {
        this.priority = priority;
        this.updatedAt = Instant.now();
    }
}
