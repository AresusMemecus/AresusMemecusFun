package com.aresus.cliper.model.clip;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "clips")
public class Clip implements Comparable<Clip>{
    @Id
    private String id; 
    private String url;
    private String embedUrl;
    private String broadcasterId;
    private String broadcasterName;
    private String creatorId;
    private String creatorName;
    private String videoId;
    private String gameId;
    private String language;
    private String title;
    private int viewCount;
    private LocalDateTime createdAt;
    private String thumbnailUrl;
    private double duration;
    private Integer vodOffset;
    private Boolean isFeatured;

    @Override
    public int compareTo(Clip other) {
        return this.id.compareTo(other.id); // Пример сравнения по id
    }
}
