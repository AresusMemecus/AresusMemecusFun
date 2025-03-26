package com.aresus.cliper.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.aresus.cliper.config.CliperConfig;
import com.aresus.cliper.model.broadcaster.Broadcaster;
import com.aresus.cliper.model.broadcaster.BroadcasterResponse;
import com.aresus.cliper.repository.BroadcasterRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class BroadcasterService {

    private static final String TWITCH_GET_USERS_URL = "https://api.twitch.tv/helix/users?id=<USER_IDS>";
    private final CliperConfig config;

    private final TokenService tokenService;
    private final BroadcasterRepository broadcasterRepository;

    public BroadcasterService(CliperConfig config, TokenService tokenService, BroadcasterRepository broadcasterRepository) {
        this.config = config;
        this.tokenService = tokenService;
        this.broadcasterRepository = broadcasterRepository;
    }

    public List<Broadcaster> fetchUsersInfoFromApi(List<String> userIds) {
        if (userIds.isEmpty()) {
            System.out.println("User ID list is empty. Ending processing.");
            return new ArrayList<>();
        }
    
        String token = tokenService.getValidAccessToken();
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
    
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("Client-Id", config.getClientId());
    
        List<Broadcaster> allBroadcasters = new ArrayList<>();
    
        // Build URL with all user IDs separated by &id=
        String userIdsParam = String.join("&id=", userIds);
        String url = TWITCH_GET_USERS_URL.replace("<USER_IDS>", userIdsParam);
        HttpEntity<Void> request = new HttpEntity<>(headers);
    
        try {
            System.out.println("Sending request to Twitch API: " + url);
    
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    String.class
            );
    
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                List<BroadcasterResponse> responseList = objectMapper.readValue(
                        root.get("data").toString(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, BroadcasterResponse.class)
                );
    
                List<Broadcaster> broadcasters = new ArrayList<>();
                for (BroadcasterResponse broadcasterResponse : responseList) {
                    Broadcaster broadcaster = new Broadcaster();
                    broadcaster.setId(broadcasterResponse.getId());
                    broadcaster.setLogin(broadcasterResponse.getLogin());
                    broadcaster.setDisplay_name(broadcasterResponse.getDisplay_name());
                    broadcaster.setDescription(broadcasterResponse.getDescription());
                    broadcaster.setCreated_at(broadcasterResponse.getCreated_at());
                    broadcaster.setEmail(broadcasterResponse.getEmail());
                    broadcaster.setView_count(broadcasterResponse.getView_count());
                    broadcaster.setOffline_image_url(broadcasterResponse.getOffline_image_url());
                    broadcaster.setProfile_image_url(broadcasterResponse.getProfile_image_url());
                    broadcaster.setBroadcaster_type(broadcasterResponse.getBroadcaster_type());
                    broadcaster.setType(broadcasterResponse.getType());
                    broadcasters.add(broadcaster);
                }
    
                System.out.println("Number of users received: " + broadcasters.size());
    
                // Update existing data in the DB or add new entries
                for (Broadcaster broadcaster : broadcasters) {
                    Broadcaster existingBroadcaster = broadcasterRepository.findById(broadcaster.getId()).orElse(null);
    
                    if (existingBroadcaster == null) {
                        // New user
                        broadcasterRepository.save(broadcaster);
                        System.out.println("Added new user: " + broadcaster.getLogin());
                    } else {
                        // Compare data and update if changes are found
                        boolean updated = updateBroadcaster(existingBroadcaster, broadcaster);
    
                        if (updated) {
                            broadcasterRepository.save(existingBroadcaster);
                            System.out.println("Updated user: " + broadcaster.getLogin());
                        }
                    }
                }
    
                allBroadcasters.addAll(broadcasters);
            } else {
                throw new RuntimeException("Failed to retrieve user information. HTTP Status: " + response.getStatusCode());
            }
    
        } catch (Exception e) {
            System.err.println("Error requesting users: " + e.getMessage());
            e.printStackTrace();
        }
    
        System.out.println("Processing completed. Total users: " + allBroadcasters.size());
        return allBroadcasters;
    }

    private boolean updateBroadcaster(Broadcaster existing, Broadcaster updated) {
        boolean changed = false;
    
        if (!equals(existing.getLogin(), updated.getLogin())) {
            existing.setLogin(updated.getLogin());
            changed = true;
        }
        if (!equals(existing.getDisplay_name(), updated.getDisplay_name())) {
            existing.setDisplay_name(updated.getDisplay_name());
            changed = true;
        }
        if (!equals(existing.getDescription(), updated.getDescription())) {
            existing.setDescription(updated.getDescription());
            changed = true;
        }
        if (existing.getView_count() != updated.getView_count()) {
            existing.setView_count(updated.getView_count());
            changed = true;
        }
        if (!equals(existing.getOffline_image_url(), updated.getOffline_image_url())) {
            existing.setOffline_image_url(updated.getOffline_image_url());
            changed = true;
        }
        if (!equals(existing.getProfile_image_url(), updated.getProfile_image_url())) {
            existing.setProfile_image_url(updated.getProfile_image_url());
            changed = true;
        }
        if (!equals(existing.getBroadcaster_type(), updated.getBroadcaster_type())) {
            existing.setBroadcaster_type(updated.getBroadcaster_type());
            changed = true;
        }
        if (!equals(existing.getType(), updated.getType())) {
            existing.setType(updated.getType());
            changed = true;
        }
        if (!equals(existing.getEmail(), updated.getEmail())) {
            existing.setEmail(updated.getEmail());
            changed = true;
        }
    
        return changed;
    }
    
    // Helper method to compare strings, taking into account possible null values
    private boolean equals(String s1, String s2) {
        if (s1 == null) return s2 == null;
        return s1.equals(s2);
    }

}
