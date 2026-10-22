package com.soodfolio.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "share_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    @JsonIgnore
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private PortfolioSnapshot snapshot;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "slug_alias", length = 100)
    private String slugAlias;

    @Builder.Default
    @Column(name = "custom_layout", length = 50)
    private String customLayout = "CINEMATIC_DECK";

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_slide_ids", columnDefinition = "jsonb")
    private List<String> allowedSlideIds = new ArrayList<>();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "max_views")
    private Integer maxViews;

    @Builder.Default
    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Builder.Default
    @Column(name = "is_revoked", nullable = false)
    private boolean isRevoked = false;

    @BatchSize(size = 25)
    @Builder.Default
    @OneToMany(mappedBy = "shareLink", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShareLinkTelemetry> telemetryList = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
