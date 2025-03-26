package com.aresus.cliper.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aresus.cliper.service.EventSubService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/webhook")
public class EventSubController {

    private final EventSubService eventSubService;
    private final ObjectMapper objectMapper;

    public EventSubController(EventSubService eventSubService, ObjectMapper objectMapper) {
        this.eventSubService = eventSubService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestHeader("Twitch-Eventsub-Message-Id") String messageId,
            @RequestHeader("Twitch-Eventsub-Message-Timestamp") String timestamp,
            @RequestHeader("Twitch-Eventsub-Message-Signature") String signature,
            @RequestHeader("Twitch-Eventsub-Message-Type") String messageType,
            @RequestHeader("Twitch-Eventsub-Subscription-Type") String subscriptionType,
            @RequestBody String payload) {

        System.out.println("Received webhook - Message ID: " + messageId + ", Type: " + messageType + ", Subscription Type: " + subscriptionType);
        
        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            System.out.println("Webhook payload: " + payload);
            
            JsonNode subscriptionNode = rootNode.get("subscription");
            String subscriptionId = subscriptionNode.get("id").asText();
            System.out.println("Subscription ID: " + subscriptionId);

            // Verify the request signature
            boolean isValid = eventSubService.verifySignature(messageId, timestamp, signature, payload, subscriptionId);
            System.out.println("Signature verification: " + (isValid ? "Valid" : "Invalid"));
            
            if (!isValid) {
                System.err.println("Invalid signature detected. Message: " + messageId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid signature");
            }

            // If this is a subscription verification request, return the challenge
            if ("webhook_callback_verification".equals(messageType)) {
                String challenge = rootNode.get("challenge").asText();
                System.out.println("Webhook verification challenge received: " + challenge);
                return ResponseEntity.ok(challenge);
            }

            // Process stream events
            if ("notification".equals(messageType)) {
                JsonNode eventNode = rootNode.get("event");
                String broadcasterId = eventNode.get("broadcaster_user_id").asText();
                
                boolean isLive = "stream.online".equals(subscriptionType);
                System.out.println("Processing notification - Broadcaster ID: " + broadcasterId + ", Is Live: " + isLive);
                
                eventSubService.handleStreamStatusChange(subscriptionType, broadcasterId, isLive);
                
                System.out.println("Event processed successfully for " + broadcasterId);
                return ResponseEntity.ok("Event processed");
            }

            return ResponseEntity.ok("Acknowledged");
        } catch (Exception e) {
            System.err.println("Error processing webhook: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing webhook");
        }
    }
}
