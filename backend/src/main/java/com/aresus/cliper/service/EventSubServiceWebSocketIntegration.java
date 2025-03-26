package com.aresus.cliper.service;

import java.security.SecureRandom;

import org.springframework.web.client.RestTemplate;

import com.aresus.cliper.repository.BroadcasterRepository;
import com.aresus.cliper.repository.EventSubRepository;
import com.aresus.cliper.websocket.StreamStatusHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EventSubServiceWebSocketIntegration {

    private final EventSubRepository eventSubRepository;
    private final BroadcasterRepository broadcasterRepository;
    private final TokenService tokenService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final StreamStatusHandler streamStatusHandler;

    public EventSubServiceWebSocketIntegration(
            EventSubRepository eventSubRepository,
            BroadcasterRepository broadcasterRepository,
            TokenService tokenService,
            ObjectMapper objectMapper,
            StreamStatusHandler streamStatusHandler) {
        this.eventSubRepository = eventSubRepository;
        this.broadcasterRepository = broadcasterRepository;
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
        this.streamStatusHandler = streamStatusHandler;
    }

    public void handleStreamStatusChange(String type, String broadcasterId, boolean isLive) {
        System.out.println("Stream status changed for broadcaster " + broadcasterId + ": " + 
                (isLive ? "online" : "offline"));
        
        streamStatusHandler.broadcastStreamStatus(broadcasterId, isLive);
    }

    private String generateSecret() {
        byte[] randomBytes = new byte[16];
        new SecureRandom().nextBytes(randomBytes);
        return bytesToHex(randomBytes);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
