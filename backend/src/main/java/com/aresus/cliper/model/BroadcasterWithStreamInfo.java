package com.aresus.cliper.model;

import com.aresus.cliper.model.broadcaster.Broadcaster;
import com.aresus.cliper.model.clip.ClipStats;

import lombok.Data;

@Data
public class BroadcasterWithStreamInfo {
    private Broadcaster broadcaster;
    private ClipStats clipStats;
    private StreamStatusCheck streamInfo;
    private boolean isLive;
    
    public BroadcasterWithStreamInfo(Broadcaster broadcaster,ClipStats clipStats, StreamStatusCheck streamInfo) {
        this.broadcaster = broadcaster;
        this.clipStats = clipStats;
        this.streamInfo = streamInfo;
        this.isLive = (streamInfo != null && streamInfo.isLive());
    }
} 