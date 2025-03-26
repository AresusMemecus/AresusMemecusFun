package com.aresus.cliper.model.broadcaster;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class BroadcasterResponse {
    @JsonProperty("id")
    private String id;
    @JsonProperty("display_name") 
    private String display_name;
    @JsonProperty("login") 
    private String login;
    @JsonProperty("created_at") 
    private String created_at;
    @JsonProperty("email") 
    private String email;
    @JsonProperty("view_count") 
    private Integer view_count;
    @JsonProperty("offline_image_url") 
    private String offline_image_url;
    @JsonProperty("profile_image_url") 
    private String profile_image_url;
    @JsonProperty("description") 
    private String description;
    @JsonProperty("broadcaster_type") 
    private String broadcaster_type;
    @JsonProperty("type") 
    private String type;
}
