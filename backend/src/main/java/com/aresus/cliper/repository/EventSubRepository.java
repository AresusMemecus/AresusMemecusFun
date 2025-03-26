package com.aresus.cliper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aresus.cliper.model.EventSubSubscription;

import java.util.List;

@Repository
public interface EventSubRepository extends JpaRepository<EventSubSubscription, String> {
    
    List<EventSubSubscription> findByType(String type);
    
    List<EventSubSubscription> findByBroadcasterId(String broadcasterId);
    
    List<EventSubSubscription> findByTypeAndBroadcasterId(String type, String broadcasterId);
    
    boolean existsByTypeAndBroadcasterId(String type, String broadcasterId);
} 