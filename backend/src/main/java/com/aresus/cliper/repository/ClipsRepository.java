package com.aresus.cliper.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.aresus.cliper.model.clip.Clip;

@Repository
public interface ClipsRepository extends JpaRepository<Clip, String> {

    List<Clip> findBybroadcasterId(String broadcasterId);

    List<Clip> findByCreatorId(String creatorId);

    @Query("SELECT c FROM Clip c WHERE c.broadcasterId = :broadcasterId " +
           "AND (:startDate IS NULL OR c.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR c.createdAt <= :endDate)")
    Page<Clip> findByBroadcasterIdWithFilters(
        @Param("broadcasterId") String broadcasterId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    @Query("SELECT c FROM Clip c WHERE c.broadcasterId = :broadcasterId " +
           "AND (:startDate IS NULL OR c.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR c.createdAt <= :endDate) " +
           "ORDER BY c.createdAt DESC")
    List<Clip> findByBroadcasterIdAndDateRange(
        @Param("broadcasterId") String broadcasterId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    List<Clip> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Transactional(readOnly = true)
    @Query("SELECT MIN(c.createdAt) FROM Clip c WHERE c.broadcasterId = :broadcasterId")
    LocalDateTime findEarliestClipDateByBroadcasterId(@Param("broadcasterId") String broadcasterId);

}
