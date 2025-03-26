package com.aresus.cliper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.aresus.cliper.model.StreamStatusCheck;

@Repository
public interface StreamStatusCheckRepository extends JpaRepository<StreamStatusCheck, Long> {
    // Удаляет все старые записи для конкретного стримера
    @Modifying
    @Transactional
    @Query("DELETE FROM StreamStatusCheck s WHERE s.broadcasterId = ?1")
    void deleteAllByBroadcasterId(String broadcasterId);
    
    // Находит последнюю проверку для стримера
    StreamStatusCheck findFirstByBroadcasterIdOrderByCheckTimeDesc(String broadcasterId);
} 