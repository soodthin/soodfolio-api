package com.soodfolio.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "share_link_telemetry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareLinkTelemetry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "share_link_id", nullable = false)
    @JsonIgnore
    private ShareLink shareLink;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "slide_key", nullable = false, length = 100)
    private String slideKey;

    @Builder.Default
    @Column(name = "dwell_seconds", nullable = false)
    private int dwellSeconds = 0;

    @Column(name = "device_type", length = 20)
    private String deviceType;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
