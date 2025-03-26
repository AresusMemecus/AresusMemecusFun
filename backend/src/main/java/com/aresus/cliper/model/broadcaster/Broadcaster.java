package com.aresus.cliper.model.broadcaster;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "broadcaster")
public class Broadcaster {
    @Id
    private String id; 
    private String display_name;
    private String login;
    private String created_at;
    private String email;
    private Integer view_count;
    private String offline_image_url;
    private String profile_image_url;
    @Column(length = 300)
    private String description;
    private String broadcaster_type;
    private String type;
}

