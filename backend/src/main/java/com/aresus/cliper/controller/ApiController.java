package com.aresus.cliper.controller;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.aresus.cliper.model.BroadcasterWithStreamInfo;
import com.aresus.cliper.model.broadcaster.Broadcaster;
import com.aresus.cliper.model.clip.ClipStats;
import com.aresus.cliper.scheduler.Scheduler;
import com.aresus.cliper.service.SortService;
import com.aresus.cliper.service.StatsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public final class ApiController {

    private final SortService sortService;
    private final StatsService statsService;
    private final Scheduler scheduler;
    
    @GetMapping("/clips/status")
    public ResponseEntity<?> getClipsUpdateStatus() {
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

    @GetMapping("/broadcasters")
    public List<Broadcaster> getAllBroadcasters() {
        return sortService.getAllBroadcasters();
    }
    
    @GetMapping("/broadcasters/with-stream-info")
    public List<BroadcasterWithStreamInfo> getAllBroadcastersWithStreamInfo() {
        return sortService.getAllBroadcastersWithStreamInfo();
    }
    
    @GetMapping("/broadcasters/{id}")
    public ResponseEntity<?> getBroadcasterById(@PathVariable String id) {
        BroadcasterWithStreamInfo broadcaster = sortService.getBroadcasterWithStreamInfoById(id);
        if (broadcaster == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(broadcaster);
    }

    @GetMapping("/clips/{id}")
    @ResponseBody
    public ResponseEntity<?> getAllClips(
            @PathVariable final String id,
            @RequestParam(required = false) final String startDate,
            @RequestParam(required = false) final String endDate,
            @RequestParam(required = false) final List<String> sort) {
    
        final LocalDateTime startDateTime = parseStartDate(startDate);
        final LocalDateTime endDateTime = parseEndDate(endDate);
    
        // Если обе даты заданы, проверяем, чтобы период не превышал 2 месяца
        if (startDateTime != null && endDateTime != null) {
            long monthsBetween = ChronoUnit.MONTHS.between(
                    startDateTime.toLocalDate().withDayOfMonth(1),
                    endDateTime.toLocalDate().withDayOfMonth(1)
            );
            if (monthsBetween > 2) {
                return ResponseEntity.badRequest().body("Период не может превышать 2 месяца");
            }
        }
    
        // Если даты не указаны, берем период по умолчанию — последние 3 недели
        LocalDateTime start;
        LocalDateTime end;
        if (startDateTime == null && endDateTime == null) {
            end = LocalDateTime.now();
            start = end.minusWeeks(3);
        } else {
            start = startDateTime;
            end = endDateTime;
        }
    
        // Формируем список критериев сортировки из параметров запроса.
        // Ожидаемый формат каждого параметра: "TYPE:ORDER", например "CREATION_DATE:desc".
        List<SortService.SortCriteria> criteria = new ArrayList<>();
        if (sort != null && !sort.isEmpty()) {
            for (String sortParam : sort) {
                String[] parts = sortParam.split(":");
                String type = parts[0].toUpperCase();
                String order = parts.length > 1 ? parts[1] : "desc";
                criteria.add(new SortService.SortCriteria(type, order));
            }
        } else {
            // Если параметры сортировки не заданы, используем сортировку по дате создания (убыванию)
            criteria.add(new SortService.SortCriteria("CREATION_DATE", "desc"));
        }
    
        return ResponseEntity.ok(sortService.getClipsSorted(id, criteria, start, end));
    }
    
    private LocalDateTime parseStartDate(final String startDate) {
        if (startDate == null || startDate.isEmpty()) {
            return null;
        }
        return LocalDate.parse(startDate).atStartOfDay();
    }

    private LocalDateTime parseEndDate(final String endDate) {
        if (endDate == null || endDate.isEmpty()) {
            return null;
        }
        return LocalDate.parse(endDate).atTime(23, 59, 59);
    }

    @GetMapping("/stats/{broadcasterId}")
    public ClipStats getBroadcasterStats(@PathVariable String broadcasterId) {  
        
        return statsService.getStats(broadcasterId);  
    }
}
