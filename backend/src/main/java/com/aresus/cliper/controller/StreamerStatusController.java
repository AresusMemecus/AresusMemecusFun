package com.aresus.cliper.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aresus.cliper.repository.BroadcasterRepository;
import com.aresus.cliper.service.EventSubService;

@RestController
@RequestMapping("/api/streamers")
public class StreamerStatusController {

    private static final Logger logger = LoggerFactory.getLogger(StreamerStatusController.class);
    
    private final BroadcasterRepository broadcasterRepository;
    private final EventSubService eventSubService;
    
    // Хранилище статусов стримеров
    private static final Map<String, Boolean> streamerStatuses = new ConcurrentHashMap<>();

    public StreamerStatusController(BroadcasterRepository broadcasterRepository, EventSubService eventSubService) {
        this.broadcasterRepository = broadcasterRepository;
        this.eventSubService = eventSubService;
    }
    
    @GetMapping("")
    public ResponseEntity<String> getRoot() {
        return ResponseEntity.ok("Корневой эндпоинт стримеров работает");
    }
    
    @GetMapping("/status")
    public ResponseEntity<List<StreamerStatus>> getAllStatuses() {
        logger.info("Получен запрос к /api/streamers/status");
        List<StreamerStatus> statuses = new ArrayList<>();
        
        // Возвращаем текущие известные статусы
        for (Map.Entry<String, Boolean> entry : streamerStatuses.entrySet()) {
            statuses.add(new StreamerStatus(entry.getKey(), entry.getValue()));
        }
        
        return ResponseEntity.ok(statuses);
    }
    
    // Статический метод для обновления статуса стримера
    public static void updateStreamerStatus(String broadcasterId, boolean isLive) {
        streamerStatuses.put(broadcasterId, isLive);
    }
    
    // Класс для представления статуса стримера
    public static class StreamerStatus {
        private String broadcasterId;
        private boolean isLive;
        
        public StreamerStatus(String broadcasterId, boolean isLive) {
            this.broadcasterId = broadcasterId;
            this.isLive = isLive;
        }
        
        public String getBroadcasterId() {
            return broadcasterId;
        }
        
        public boolean isLive() {
            return isLive;
        }
    }
} 