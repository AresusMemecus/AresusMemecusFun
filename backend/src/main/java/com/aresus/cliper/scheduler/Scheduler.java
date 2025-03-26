package com.aresus.cliper.scheduler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.aresus.cliper.service.StatsService;
import com.aresus.cliper.websocket.ClipsBroadcasterHandler;
import com.aresus.cliper.websocket.ClipsTimerHandler;
import com.aresus.cliper.model.broadcaster.Broadcaster;
import com.aresus.cliper.repository.BroadcasterRepository;
import com.aresus.cliper.service.BroadcasterService;
import com.aresus.cliper.service.ClipsService;

@Service
public class Scheduler {

    private final BroadcasterRepository broadcasterRepository;
    private final ClipsService clipsService;
    private final BroadcasterService broadcasterService;
    private LocalDateTime nextUpdateTime;
    private volatile boolean isUpdating = false;
    private final StatsService statsService;

    public Scheduler(BroadcasterRepository broadcasterRepository,
                     ClipsService clipsService,
                     BroadcasterService broadcasterService,
                     StatsService statsService) {
        this.broadcasterRepository = broadcasterRepository;
        this.clipsService = clipsService;
        this.broadcasterService = broadcasterService;
        this.statsService = statsService;
        this.nextUpdateTime = LocalDateTime.now().plusMinutes(20);
    }

    public LocalDateTime getNextUpdateTime() {
        return nextUpdateTime;
    }

    public boolean isUpdating() {
        return isUpdating;
    }

    @Scheduled(fixedRate = 20 * 60 * 1000)
    public void fetchClipsForAllStreamers() {
        try {
            isUpdating = true;
            int secondsUntilNextUpdate = (int) Duration.between(LocalDateTime.now(), nextUpdateTime).getSeconds();
            ClipsTimerHandler.broadcastStatus(isUpdating, secondsUntilNextUpdate);

            List<String> broadcasterIds = getBroadcasterIds();
            for (String broadcasterId : broadcasterIds) {
                boolean success = false;
                while (!success) {
                    ClipsBroadcasterHandler.broadcastBroadcasterStatus(broadcasterId, true);
                    try {
                        clipsService.getAllClipsFromApi(broadcasterId);
                        System.out.println("Request completed for " + broadcasterId);
                        ClipsBroadcasterHandler.broadcastBroadcasterStatus(broadcasterId, false);
                        success = true;
                    } catch (Exception e) {
                        System.err.println("Error fetching clips for " + broadcasterId + ": " + e.getMessage());
                        Thread.sleep(5000);
                    }
                }
            }
            
            // Update statistics for all streamers after fetching clips
            statsService.updateStats(null);
            System.out.println("Streamer statistics updated after fetching clips");
        } 
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } 
        finally {
            isUpdating = false;
            nextUpdateTime = LocalDateTime.now().plusMinutes(20);
            int secondsUntilNextUpdate = (int) Duration.between(LocalDateTime.now(), nextUpdateTime).getSeconds();
            ClipsTimerHandler.broadcastStatus(isUpdating, secondsUntilNextUpdate);
        }
    }

    @Scheduled(initialDelay = 60 * 60 * 1000, fixedRate = 60 * 60 * 1000)
    public void fetchInfoForAllStreamers() {
        List<String> broadcasterIds = getBroadcasterIds();

        System.out.println("Starting fetchInfoForAllStreamers task...");
    
        // Split the list into several parts if necessary
        int batchSize = 100; // Approximately 10 users at a time
        for (int i = 0; i < broadcasterIds.size(); i += batchSize) {
            List<String> batch = broadcasterIds.subList(i, Math.min(i + batchSize, broadcasterIds.size()));
            
            boolean success = false;
            while (!success) {
                try {
                    // Send the list of users
                    broadcasterService.fetchUsersInfoFromApi(batch);
                    System.out.println("Request completed for broadcasters: " + batch);
                    success = true;
                } catch (Exception e) {
                    System.out.println("Error fetching info for broadcasters " + batch + ": " + e.getMessage());
                    try {
                        Thread.sleep(5000); // Retry delay
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }
    
    private List<String> getBroadcasterIds() {
        return broadcasterRepository.findAll().stream()
                .map(Broadcaster::getId)
                .collect(Collectors.toList());
    }
}
