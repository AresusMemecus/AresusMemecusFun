package com.aresus.cliper.service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.aresus.cliper.config.CliperConfig;
import com.aresus.cliper.model.clip.Clip;
import com.aresus.cliper.model.clip.ClipData;
import com.aresus.cliper.model.clip.ClipResponse;
import com.aresus.cliper.repository.ClipsRepository;

@Service
public class ClipsService {

    private static final String TWITCH_GETCLIPS_URL = "https://api.twitch.tv/helix/clips?broadcaster_id=<BROADCASTER_ID>&first=100&after=<AFTER_CURSOR>&started_at=<STARTED_AT>&ended_at=<ENDED_AT>";
    private static final int WEEKS_TO_FETCH = 3; // Number of weeks to fetch

    private final TokenService tokenService;
    private final ClipsRepository clipRepository;
    private final CliperConfig config;

    public ClipsService(TokenService tokenService, ClipsRepository clipRepository, CliperConfig config) {
        this.tokenService = tokenService;
        this.clipRepository = clipRepository;
        this.config = config;
    }

    @Transactional
    public List<Clip> getAllClipsFromApi(String broadcasterId) {
        List<Clip> allClips = new ArrayList<>();
        OffsetDateTime currentTime = OffsetDateTime.now();

        // Fetch clips for the last 3 weeks
        for (int week = 0; week < WEEKS_TO_FETCH; week++) {
            OffsetDateTime endDate = currentTime.minusWeeks(week);
            OffsetDateTime startDate = endDate.minusWeeks(1);

            System.out.println("Fetching clips for the period: " +
                startDate.format(DateTimeFormatter.ISO_DATE) + " - " +
                endDate.format(DateTimeFormatter.ISO_DATE));

            List<Clip> weekClips = getClipsForTimeWindow(broadcasterId, startDate, endDate);
            allClips.addAll(weekClips);
        }

        return allClips;
    }

    private List<Clip> getClipsForTimeWindow(String broadcasterId, OffsetDateTime startDate, OffsetDateTime endDate) {
        String token = tokenService.getValidAccessToken();
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("Client-Id", config.getClientId());

        List<Clip> windowClips = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String cursor = "";
        boolean hasMore = true;
        int pageCount = 0;

        while (hasMore) {
            pageCount++;
            System.out.println("Fetching page " + pageCount + " of clips" +
                (cursor.isEmpty() ? "" : " (cursor: " + cursor + ")"));

            String url = TWITCH_GETCLIPS_URL
                    .replace("<BROADCASTER_ID>", broadcasterId)
                    .replace("<AFTER_CURSOR>", cursor)
                    .replace("<STARTED_AT>", startDate.format(formatter))
                    .replace("<ENDED_AT>", endDate.format(formatter));

            try {
                ResponseEntity<ClipResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        ClipResponse.class
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    updateRateLimits(response.getHeaders());

                    ClipResponse clipsResponse = response.getBody();
                    System.out.println("Received " + clipsResponse.getData().size() +
                        " clips on page " + pageCount);

                    List<Clip> clipEntities = clipsResponse.getData().stream()
                            .map(this::mapToClipEntity)
                            .collect(Collectors.toList());

                    // Save all clips without checking for duplicates
                    clipRepository.saveAll(clipEntities);
                    windowClips.addAll(clipEntities);

                    cursor = clipsResponse.getPagination() != null ?
                        clipsResponse.getPagination().getCursor() : null;
                    hasMore = cursor != null && !clipsResponse.getData().isEmpty();

                    if (hasMore) {
                        System.out.println("There is a next page, cursor: " + cursor);
                    } else {
                        System.out.println("Reached the end of the clip list");
                    }
                }
            } catch (Exception e) {
                System.err.println("Error fetching page " + pageCount + ": " + e.getMessage());
                hasMore = false;
            }
        }

        System.out.println("Total clips fetched for the period " + startDate + " - " + endDate + ": " + windowClips.size());
        return windowClips;
    }

    private void updateRateLimits(HttpHeaders headers) {
        try {
            List<String> limitHeaders = headers.get("Ratelimit-Limit");
            List<String> remainingHeaders = headers.get("Ratelimit-Remaining");
            List<String> resetHeaders = headers.get("Ratelimit-Reset");

            System.out.println("Rate Limits Headers:");
            System.out.println("Limit: " + limitHeaders);
            System.out.println("Remaining: " + remainingHeaders);
            System.out.println("Reset: " + resetHeaders);

            if (remainingHeaders != null && !remainingHeaders.isEmpty() &&
                resetHeaders != null && !resetHeaders.isEmpty()) {

                int remaining = Integer.parseInt(remainingHeaders.get(0));
                long resetTimestamp = Long.parseLong(resetHeaders.get(0));

                if (remaining == 0) {
                    long currentTime = System.currentTimeMillis() / 1000L;
                    long waitTimeSeconds = resetTimestamp - currentTime;

                    if (waitTimeSeconds > 0) {
                        System.out.println("Rate limit reached. Waiting " + waitTimeSeconds +
                            " seconds until rate limit resets");
                        Thread.sleep(waitTimeSeconds * 1000);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing rate limits: " + e.getMessage());
        }
    }

    private Clip mapToClipEntity(ClipData clipData) {
        Clip clipEntity = new Clip();
        clipEntity.setId(clipData.getId());
        clipEntity.setUrl(clipData.getUrl());
        clipEntity.setEmbedUrl(clipData.getEmbedUrl());
        clipEntity.setBroadcasterId(clipData.getBroadcasterId());
        clipEntity.setBroadcasterName(clipData.getBroadcasterName());
        clipEntity.setCreatorId(clipData.getCreatorId());
        clipEntity.setCreatorName(clipData.getCreatorName());
        clipEntity.setVideoId(clipData.getVideoId());
        clipEntity.setGameId(clipData.getGameId());
        clipEntity.setLanguage(clipData.getLanguage());
        clipEntity.setTitle(clipData.getTitle());
        clipEntity.setViewCount(clipData.getViewCount());
        clipEntity.setCreatedAt(OffsetDateTime.parse(clipData.getCreatedAt(),
            DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime());
        clipEntity.setThumbnailUrl(clipData.getThumbnailUrl());
        clipEntity.setDuration(clipData.getDuration());
        clipEntity.setVodOffset(clipData.getVodOffset());
        clipEntity.setIsFeatured(clipData.getIsFeatured());
        return clipEntity;
    }
}
