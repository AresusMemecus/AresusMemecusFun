package com.aresus.cliper.websocket;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class StreamStatusHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        System.out.println("WebSocket connection established: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        System.out.println("WebSocket connection closed: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            JsonNode jsonNode = objectMapper.readTree(payload);
            
            // Check the type of message
            if (jsonNode.has("action") && "GET_ALL_STATUSES".equals(jsonNode.get("action").asText())) {
                // Just log the request without performing heavy operations
                System.out.println("Received request to update streamer statuses");
                // Another component will handle the request via an event
            }
        } catch (Exception e) {
            System.err.println("Error processing WebSocket message: " + e.getMessage());
        }
    }

    public void broadcastStreamStatus(String broadcasterId, boolean isLive) {
        Map<String, Object> message = Map.of(
            "Status", isLive,
            "broadcasterIds", broadcasterId 
        );

        try {
            String json = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(json);

            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        } catch (IOException e) {
            System.err.println("Error sending stream status: " + e.getMessage());
        }
    }
}