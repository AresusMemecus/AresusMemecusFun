package com.aresus.cliper.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.aresus.cliper.service.EventSubService;

@Component
public class EventSubScheduler {

    private final EventSubService eventSubService;

    public EventSubScheduler(EventSubService eventSubService) {
        this.eventSubService = eventSubService;
    }

    /**
     * Runs at application startup and then every 24 hours
     * to update event subscriptions.
     */
    @Scheduled(initialDelay = 5 * 1000, fixedRate = 24 * 60 * 60 * 1000)
    public void updateEventSubSubscriptionsAndCheckStatus() {
        System.out.println("Starting EventSub subscription update scheduler and status check");
        eventSubService.subscribeAllBroadcasters(); 
        eventSubService.checkAllStreamStatuses();
    }

    /**
     * New method for periodic checks (for example, every 5 minutes)
     */
    @Scheduled(initialDelay = 30 * 1000, fixedRate = 5 * 60 * 1000)
    public void periodicStreamStatusCheck() {
        System.out.println("Performing periodic stream status check");
        eventSubService.checkAllStreamStatuses();
    }
}
