package com.aresus.cliper.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "stream_status_checks")
public class StreamStatusCheck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String broadcasterId;
    private boolean live;
    private LocalDateTime checkTime;
    private String streamTitle;
    private String gameName;
    private Integer viewerCount;
    private String thumbnailUrl;
    private String language;
} 