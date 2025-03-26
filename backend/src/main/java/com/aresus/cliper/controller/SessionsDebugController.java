package com.aresus.cliper.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aresus.cliper.model.StreamSession;
import com.aresus.cliper.model.StreamStatusCheck;
import com.aresus.cliper.model.broadcaster.Broadcaster;
import com.aresus.cliper.repository.BroadcasterRepository;
import com.aresus.cliper.repository.StreamSessionRepository;
import com.aresus.cliper.repository.StreamStatusCheckRepository;
import com.aresus.cliper.service.EventSubService;

@RestController
@RequestMapping("/api/debug/sessions")
public class SessionsDebugController {

    private final StreamSessionRepository streamSessionRepository;
    private final StreamStatusCheckRepository streamStatusCheckRepository;
    private final BroadcasterRepository broadcasterRepository;
    private final EventSubService eventSubService;

    public SessionsDebugController(
            StreamSessionRepository streamSessionRepository,
            StreamStatusCheckRepository streamStatusCheckRepository,
            BroadcasterRepository broadcasterRepository,
            EventSubService eventSubService) {
        this.streamSessionRepository = streamSessionRepository;
        this.streamStatusCheckRepository = streamStatusCheckRepository;
        this.broadcasterRepository = broadcasterRepository;
        this.eventSubService = eventSubService;
    }

    @GetMapping
    public ResponseEntity<List<StreamSession>> getAllSessions() {
        return ResponseEntity.ok(streamSessionRepository.findAll());
    }

    @GetMapping("/broadcasters")
    public ResponseEntity<List<Broadcaster>> getAllBroadcasters() {
        return ResponseEntity.ok(broadcasterRepository.findAll());
    }

    @GetMapping("/status-checks")
    public ResponseEntity<List<StreamStatusCheck>> getAllStatusChecks() {
        return ResponseEntity.ok(streamStatusCheckRepository.findAll());
    }

    @GetMapping("/broadcaster/{id}")
    public ResponseEntity<Map<String, Object>> getBroadcasterInfo(@PathVariable String id) {
        Broadcaster broadcaster = broadcasterRepository.findById(id).orElse(null);
        if (broadcaster == null) {
            return ResponseEntity.notFound().build();
        }
        
        StreamStatusCheck latestCheck = streamStatusCheckRepository
            .findFirstByBroadcasterIdOrderByCheckTimeDesc(id);
        
        List<StreamSession> sessions = streamSessionRepository.findByBroadcasterIdOrderByIdDesc(id);
        
        return ResponseEntity.ok(Map.of(
            "broadcaster", broadcaster,
            "latestCheck", latestCheck != null ? latestCheck : "No status checks found",
            "sessions", sessions
        ));
    }

    @PostMapping("/force-check")
    public ResponseEntity<String> forceStreamCheck() {
        try {
            eventSubService.checkAllStreamStatuses();
            return ResponseEntity.ok("Stream status check completed");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/create-test-session/{broadcasterId}")
    public ResponseEntity<StreamSession> createTestSession(@PathVariable String broadcasterId) {
        Broadcaster broadcaster = broadcasterRepository.findById(broadcasterId).orElse(null);
        if (broadcaster == null) {
            return ResponseEntity.notFound().build();
        }
        
        StreamSession session = new StreamSession();
        session.setBroadcasterId(broadcasterId);
        session.setSessionType("TEST");
        session.setMessage("Test session created manually");
        session.setStartTime(LocalDateTime.now());
        
        return ResponseEntity.ok(streamSessionRepository.save(session));
    }

    @PostMapping("/test-session-service/{broadcasterId}")
    public ResponseEntity<StreamSession> testSessionService(@PathVariable String broadcasterId) {
        Broadcaster broadcaster = broadcasterRepository.findById(broadcasterId).orElse(null);
        if (broadcaster == null) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            StreamSession session = eventSubService.createTestSession(broadcasterId);
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            System.err.println("Error in test session service: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/update-subscriptions")
    public ResponseEntity<String> updateEventSubSubscriptions() {
        try {
            eventSubService.subscribeAllBroadcasters();
            return ResponseEntity.ok("EventSub subscriptions updated");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
    
    @GetMapping("/settings")
    public ResponseEntity<Map<String, Object>> getSettings() {
        try {
            String webhookUrl = System.getProperty("app.webhook-url");
            if (webhookUrl == null) {
                webhookUrl = "Property not found";
            }
            
            return ResponseEntity.ok(Map.of(
                "webhookUrl", webhookUrl,
                "clientId", System.getProperty("string.CLIENT_ID", "Property not found"),
                "time", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
} 