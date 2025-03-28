package com.aresus.cliper.model.clip;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ClipData {
    @JsonProperty("id")
    private String id;
    @JsonProperty("url")
    private String url;
    @JsonProperty("embed_url")
    private String embedUrl;
    @JsonProperty("broadcaster_id")
    private String broadcasterId;
    @JsonProperty("broadcaster_name")
    private String broadcasterName;
    @JsonProperty("creator_id")
    private String creatorId;
    @JsonProperty("creator_name")
    private String creatorName;
    @JsonProperty("video_id")
    private String videoId;
    @JsonProperty("game_id")
    private String gameId;
    @JsonProperty("language")
    private String language;
    @JsonProperty("title")
    private String title;
    @JsonProperty("view_count")
    private Integer viewCount;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("thumbnail_url")
    private String thumbnailUrl;
    @JsonProperty("duration")
    private Double duration;
    @JsonProperty("vod_offset")
    private Integer vodOffset;
    @JsonProperty("is_featured")
    private Boolean isFeatured;
} 