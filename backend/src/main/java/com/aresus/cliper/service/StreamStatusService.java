package com.aresus.cliper.service;

import org.springframework.stereotype.Service;

import com.aresus.cliper.controller.StreamerStatusController;
import com.aresus.cliper.repository.BroadcasterRepository;
import com.aresus.cliper.websocket.StreamStatusHandler;

@Service
public class StreamStatusService {
    
    private final StreamStatusHandler streamStatusHandler;
    private final BroadcasterRepository broadcasterRepository;
    
    public StreamStatusService(StreamStatusHandler streamStatusHandler,
                             BroadcasterRepository broadcasterRepository) {
        this.streamStatusHandler = streamStatusHandler;
        this.broadcasterRepository = broadcasterRepository;
    }
    
    public void updateStreamStatus(String broadcasterId, boolean isLive) {
        // Обновляем статус через WebSocket
        streamStatusHandler.broadcastStreamStatus(broadcasterId, isLive);
        
        // Обновляем статус в контроллере
        StreamerStatusController.updateStreamerStatus(broadcasterId, isLive);
    }
    
    public void checkAllStreamStatuses() {
        // Этот метод будет вызываться из EventSubService
    }
} 