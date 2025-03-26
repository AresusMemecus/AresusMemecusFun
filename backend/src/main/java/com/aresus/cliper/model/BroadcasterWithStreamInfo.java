package com.aresus.cliper.model;

import com.aresus.cliper.model.broadcaster.Broadcaster;
import lombok.Data;

@Data
public class BroadcasterWithStreamInfo {
    private Broadcaster broadcaster;
    private StreamStatusCheck streamInfo;
    private boolean isLive;
    
    public BroadcasterWithStreamInfo(Broadcaster broadcaster, StreamStatusCheck streamInfo) {
        this.broadcaster = broadcaster;
        this.streamInfo = streamInfo;
        this.isLive = (streamInfo != null && streamInfo.isLive());
    }
} 