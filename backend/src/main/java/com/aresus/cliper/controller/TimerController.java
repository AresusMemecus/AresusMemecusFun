package com.aresus.cliper.controller;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aresus.cliper.scheduler.Scheduler;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TimerController {
    
    private final Scheduler scheduler;
    
    @GetMapping("/clips/timer")
    public ResponseEntity<?> getClipTimerStatus() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextUpdate = scheduler.getNextUpdateTime();
        Duration duration = Duration.between(now, nextUpdate);
        // Если время уже прошло, возвращаем 0 секунд
        long secondsRemaining = duration.isNegative() ? 0 : duration.getSeconds();
        boolean isUpdating = scheduler.isUpdating();
        
        Map<String, Object> response = new HashMap<>();
        response.put("secondsRemaining", secondsRemaining);
        response.put("isUpdating", isUpdating);
        
        return ResponseEntity.ok(response);
    }
}
