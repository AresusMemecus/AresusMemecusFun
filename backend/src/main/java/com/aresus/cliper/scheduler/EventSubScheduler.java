package com.aresus.cliper.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.aresus.cliper.service.EventSubService;

import jakarta.annotation.PostConstruct;

@Component
public class EventSubScheduler {

    private final EventSubService eventSubService;

    public EventSubScheduler(EventSubService eventSubService) {
        this.eventSubService = eventSubService;
    }

    /**
     * Runs once at application startup.
     */
    @PostConstruct
    public void onStartup() {
        System.out.println("Running EventSub update at application startup");
        updateEventSubSubscriptionsAndCheckStatus();
    }

    /**
     * Runs every day at midnight.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void updateEventSubSubscriptionsAndCheckStatus() {
        System.out.println("Starting EventSub subscription update scheduler and status check");
        eventSubService.subscribeAllBroadcasters(); 
        eventSubService.checkAllStreamStatuses();
    }

    /**
     * New method for periodic checks (for example, every 5 minutes)
     */
    @Scheduled(cron = "0 0/5 * * * *")
    public void periodicStreamStatusCheck() {
        System.out.println("Performing periodic stream status check");
        eventSubService.checkAllStreamStatuses();
    }
    
}
