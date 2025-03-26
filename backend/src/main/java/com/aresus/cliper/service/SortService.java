package com.aresus.cliper.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.aresus.cliper.model.BroadcasterWithStreamInfo;
import com.aresus.cliper.model.StreamStatusCheck;
import com.aresus.cliper.model.broadcaster.Broadcaster;
import com.aresus.cliper.model.clip.Clip;
import com.aresus.cliper.repository.BroadcasterRepository;
import com.aresus.cliper.repository.ClipsRepository;
import com.aresus.cliper.repository.StreamStatusCheckRepository;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Service
@AllArgsConstructor
public class SortService {

    @Getter
    @AllArgsConstructor
    public static final class SortCriteria {
        private final String type;
        private final String order;
    }

    private final ClipsRepository clipRepository;
    private final BroadcasterRepository broadcasterRepository;
    private final StreamStatusCheckRepository streamStatusCheckRepository;

    public List<Broadcaster> getAllBroadcasters() {
        return broadcasterRepository.findAll();
    }
    
    public List<BroadcasterWithStreamInfo> getAllBroadcastersWithStreamInfo() {
        List<Broadcaster> broadcasters = broadcasterRepository.findAll();
        List<BroadcasterWithStreamInfo> result = new ArrayList<>();
        
        for (Broadcaster broadcaster : broadcasters) {
            // Получаем последнюю информацию о стриме для каждого стримера
            StreamStatusCheck streamInfo = streamStatusCheckRepository
                .findFirstByBroadcasterIdOrderByCheckTimeDesc(broadcaster.getId());
            
            result.add(new BroadcasterWithStreamInfo(broadcaster, streamInfo));
        }
        
        return result;
    }

    public Broadcaster getBroadcasterById(final String id) {
        return broadcasterRepository.findById(id).orElse(null); 
    }
    
    public BroadcasterWithStreamInfo getBroadcasterWithStreamInfoById(final String id) {
        Broadcaster broadcaster = broadcasterRepository.findById(id).orElse(null);
        if (broadcaster == null) {
            return null;
        }
        
        StreamStatusCheck streamInfo = streamStatusCheckRepository
            .findFirstByBroadcasterIdOrderByCheckTimeDesc(broadcaster.getId());
        
        return new BroadcasterWithStreamInfo(broadcaster, streamInfo);
    }

    public List<Clip> getClipsSorted(
            final String broadcasterId,
            final List<SortCriteria> sortCriteria,
            final LocalDateTime startDate,
            final LocalDateTime endDate) {

        // Получаем все клипы для вещателя за указанный период
        List<Clip> clips = clipRepository.findByBroadcasterIdAndDateRange(broadcasterId, startDate, endDate);

        // Строим компаратор на основе переданных критериев сортировки
        Comparator<Clip> comparator = null;
        if (sortCriteria != null && !sortCriteria.isEmpty()) {
            for (SortCriteria criteria : sortCriteria) {
                Comparator<Clip> currentComparator;
                switch (criteria.getType()) {
                    case "VIEWS":
                        currentComparator = Comparator.comparing(Clip::getViewCount);
                        break;
                    case "CREATION_DATE":
                        currentComparator = Comparator.comparing(Clip::getCreatedAt);
                        break;
                    case "DURATION":
                        currentComparator = Comparator.comparing(Clip::getDuration);
                        break;
                    default:
                        currentComparator = Comparator.comparing(Clip::getCreatedAt);
                        break;
                }
                if ("desc".equalsIgnoreCase(criteria.getOrder())) {
                    currentComparator = currentComparator.reversed();
                }
                comparator = (comparator == null)
                        ? currentComparator
                        : comparator.thenComparing(currentComparator);
            }
        } else {
            // Если критериев не задано, сортируем по дате создания в обратном порядке
            comparator = Comparator.comparing(Clip::getCreatedAt).reversed();
        }

        clips.sort(comparator);
        return clips;
    }
}
