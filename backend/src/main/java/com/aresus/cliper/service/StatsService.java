package com.aresus.cliper.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.aresus.cliper.model.clip.Clip;
import com.aresus.cliper.model.clip.ClipStats;
import com.aresus.cliper.repository.ClipsRepository;

import jakarta.annotation.PostConstruct;

@Service
public class StatsService {

    private final ClipsRepository clipsRepository;
    private Map<String, ClipStats> statsCache = new HashMap<>();
    private LocalDateTime lastUpdate; // Время последнего обновления
    
    // Настройки кэша - время в минутах перед принудительным обновлением
    private static final long CACHE_TTL_MINUTES = 15; 

    public StatsService(ClipsRepository clipsRepository) {
        this.clipsRepository = clipsRepository;
    }

    @PostConstruct
    public void init() {
        updateStats(null); // Инициализация без параметра для загрузки всех данных
    } 

    /**
     * Получает статистику для стримера.
     * Проверяет актуальность кэша и использует его если данные свежие.
     */
    public synchronized ClipStats getStats(String broadcasterId) {
        // Проверяем, нужно ли обновлять кэш
        if (isCacheExpired()) {
            updateStats(null);
        }
        
        // Если запрашивается статистика конкретного стримера и ее нет в кэше
        if (broadcasterId != null && !statsCache.containsKey(broadcasterId)) {
            return updateStats(broadcasterId);
        }
        
        return statsCache.get(broadcasterId);
    }
    
    /**
     * Проверяет, истекло ли время жизни кэша
     */
    private boolean isCacheExpired() {
        if (lastUpdate == null) {
            return true;
        }
        
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(lastUpdate, now);
        return duration.toMinutes() > CACHE_TTL_MINUTES;
    }
    
    /**
     * Принудительно обновляет кэш для всех стримеров или конкретного стримера
     */
    public synchronized ClipStats updateStats(String broadcasterId) {
        // Получаем даты начала и конца текущей недели
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                                       .withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfWeek = now.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))
                                     .withHour(23).withMinute(59).withSecond(59);

        // Получаем клипы за текущую неделю
        List<Clip> weeklyClips;
        if (broadcasterId != null) {
            // Фильтруем по конкретному стримеру
            weeklyClips = clipsRepository.findByBroadcasterIdAndDateRange(broadcasterId, startOfWeek, endOfWeek);
        } else {
            // Получаем все клипы за неделю
            weeklyClips = clipsRepository.findByCreatedAtBetween(startOfWeek, endOfWeek);
        }

        // Если обновляем все данные, создаем новый кэш
        Map<String, ClipStats> newStats = broadcasterId == null ? new HashMap<>() : new HashMap<>(statsCache);
        
        for (Clip clip : weeklyClips) {
            String currentBroadcasterId = clip.getBroadcasterId();
            ClipStats stats = newStats.computeIfAbsent(currentBroadcasterId, k -> {
                ClipStats s = new ClipStats();
                s.setBroadcasterId(k);
                s.setWeeklyClipsCount(0);
                s.setTotalDuration(0);
                return s;
            });

            stats.setWeeklyClipsCount(stats.getWeeklyClipsCount() + 1);
            stats.setTotalDuration(stats.getTotalDuration() + clip.getDuration());
        }

        // Обновляем кэш полностью только если запрос был для всех данных
        if (broadcasterId == null) {
            this.statsCache = newStats;
        } else {
            // Иначе обновляем только данные конкретного стримера
            this.statsCache.put(broadcasterId, newStats.get(broadcasterId));
        }
        
        // Обновляем время последнего обновления
        this.lastUpdate = LocalDateTime.now();
        
        return statsCache.get(broadcasterId);
    }
    
    /**
     * Принудительно сбрасывает кэш и обновляет данные
     */
    public synchronized void forceUpdateCache() {
        this.statsCache.clear();
        this.lastUpdate = null;
        updateStats(null);
    }
}
