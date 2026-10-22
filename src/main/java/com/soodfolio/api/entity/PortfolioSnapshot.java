package com.soodfolio.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "portfolio_snapshots", uniqueConstraints = {
        @UniqueConstraint(name = "uq_portfolio_version", columnNames = {"portfolio_id", "version"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    @JsonIgnore
    private Portfolio portfolio;

    @Builder.Default
    @Column(nullable = false)
    private int version = 1;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot_data", columnDefinition = "jsonb", nullable = false)
    private String snapshotData;

    @Column(length = 255)
    private String changelog;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
