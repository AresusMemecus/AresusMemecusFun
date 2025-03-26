package com.aresus.cliper.model.clip;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ClipResponse {

    @JsonProperty("data")
    private List<ClipData> data;

    @JsonProperty("pagination")
    private Pagination pagination;

    public List<ClipData> getData() {
        return data;
    }

    public void setData(List<ClipData> data) {
        this.data = data;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    @Data
    public static class Clip {
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
        private int viewCount;
        @JsonProperty("created_at")
        private String createdAt;
        @JsonProperty("thumbnail_url")
        private String thumbnailUrl;
        @JsonProperty("duration")
        private double duration;
        @JsonProperty("vod_offset")
        private Integer vodOffset;
        @JsonProperty("is_featured")
        private Boolean isFeatured;
    }
    
    @Data
    public static class Pagination {
        @JsonProperty("cursor")
        private String cursor;

        public String getCursor() {
            return cursor;
        }

        public void setCursor(String cursor) {
            this.cursor = cursor;
        }
    }
}
