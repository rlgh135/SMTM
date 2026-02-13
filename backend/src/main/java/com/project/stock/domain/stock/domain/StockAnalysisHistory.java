package com.project.stock.domain.stock.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

/**
 * 주식 분석 이력 엔티티.
 * 일일 AI 분석 결과를 저장합니다.
 */
@Entity
@Table(
    name = "stock_analysis_history",
    indexes = {
        @Index(name = "idx_stock_analysis_date", columnList = "stock_id, analyzed_date")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_stock_date", columnNames = {"stock_id", "analyzed_date"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockAnalysisHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(name = "analyzed_date", nullable = false)
    private LocalDate analyzedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation", nullable = false, length = 10)
    private StockAnalysisResult.Recommendation recommendation;

    @Column(name = "confidence_score", nullable = false)
    private Integer confidenceScore;

    @Column(name = "technical_analysis", columnDefinition = "TEXT")
    private String technicalAnalysis;

    @Column(name = "supply_analysis", columnDefinition = "TEXT")
    private String supplyAnalysis;

    @ElementCollection
    @CollectionTable(
        name = "stock_analysis_risk_factors",
        joinColumns = @JoinColumn(name = "analysis_id")
    )
    @Column(name = "risk_factor", length = 500)
    private List<String> riskFactors;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Builder
    public StockAnalysisHistory(
        Stock stock,
        LocalDate analyzedDate,
        StockAnalysisResult.Recommendation recommendation,
        Integer confidenceScore,
        String technicalAnalysis,
        String supplyAnalysis,
        List<String> riskFactors
    ) {
        this.stock = stock;
        this.analyzedDate = analyzedDate;
        this.recommendation = recommendation;
        this.confidenceScore = confidenceScore;
        this.technicalAnalysis = technicalAnalysis;
        this.supplyAnalysis = supplyAnalysis;
        this.riskFactors = riskFactors;
        this.createdAt = Instant.now();
    }

    /**
     * StockAnalysisResult로부터 이력 엔티티를 생성합니다.
     */
    public static StockAnalysisHistory fromAnalysisResult(
        Stock stock,
        LocalDate analyzedDate,
        StockAnalysisResult result
    ) {
        return StockAnalysisHistory.builder()
            .stock(stock)
            .analyzedDate(analyzedDate)
            .recommendation(result.recommendation())
            .confidenceScore(result.confidenceScore())
            .technicalAnalysis(result.technicalAnalysis())
            .supplyAnalysis(result.supplyAnalysis())
            .riskFactors(result.riskFactors())
            .build();
    }
}
