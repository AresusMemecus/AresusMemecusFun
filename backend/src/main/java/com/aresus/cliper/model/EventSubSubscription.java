package com.aresus.cliper.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "eventsub_subscriptions")
public class EventSubSubscription {
    @Id
    @Column(nullable = false)
    private String id;
    @Column(nullable = false)
    private String type;
    @Column(nullable = false)
    private String version;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false)
    private String broadcasterId;
    @Column(nullable = false)
    private String broadcasterLogin;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    @Column(nullable = false)
    private String callbackUrl;
    @Column(nullable = false)
    private String transportMethod;
    @Column(nullable = false)
    private Integer cost;
    @Column(nullable = false)
    private String secret;
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
