package com.soodfolio.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "analytics_views")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsView {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    @JsonIgnore
    private Portfolio portfolio;

    @Column(name = "slide_id", length = 100)
    private String slideId;

    @Builder.Default
    @Column(name = "event_type", length = 50)
    private String eventType = "VIEW";

    @Builder.Default
    @Column(name = "duration_seconds")
    private int durationSeconds = 0;

    @Column(name = "clicked_link", columnDefinition = "TEXT")
    private String clickedLink;

    @Column(columnDefinition = "TEXT")
    private String referrer;

    @Column(length = 100)
    private String country;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
