package com.aresus.cliper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aresus.cliper.model.StreamSession;
import java.util.List;

@Repository
public interface StreamSessionRepository extends JpaRepository<StreamSession, Long> {
    // Метод для получения сессий конкретного стримера в порядке убывания ID
    List<StreamSession> findByBroadcasterIdOrderByIdDesc(String broadcasterId);
} 