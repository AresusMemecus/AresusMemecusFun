package com.aresus.cliper.service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.aresus.cliper.model.EventSubSubscription;
import com.aresus.cliper.model.StreamSession;
import com.aresus.cliper.model.StreamStatusCheck;
import com.aresus.cliper.model.broadcaster.Broadcaster;
import com.aresus.cliper.repository.BroadcasterRepository;
import com.aresus.cliper.repository.EventSubRepository;
import com.aresus.cliper.repository.StreamSessionRepository;
import com.aresus.cliper.repository.StreamStatusCheckRepository;
import com.aresus.cliper.websocket.StreamStatusHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EventSubService {

    private final EventSubRepository eventSubRepository;
    private final BroadcasterRepository broadcasterRepository;
    private final StreamSessionRepository streamSessionRepository;
    private final StreamStatusCheckRepository streamStatusCheckRepository;
    private final TokenService tokenService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final StreamStatusHandler streamStatusHandler;
    
    @Value("${string.CLIENT_ID}")
    private String clientId;
    
    @Value("${app.webhook-url}")
    private String webhookUrl;
    
    private static final String TWITCH_EVENTSUB_API = "https://api.twitch.tv/helix/eventsub/subscriptions";
    private static final String STREAM_ONLINE_TYPE = "stream.online";
    private static final String STREAM_OFFLINE_TYPE = "stream.offline";
    private static final String VERSION = "1";

    public EventSubService(
            EventSubRepository eventSubRepository,
            BroadcasterRepository broadcasterRepository,
            StreamSessionRepository streamSessionRepository,
            StreamStatusCheckRepository streamStatusCheckRepository,
            TokenService tokenService,
            ObjectMapper objectMapper,
            StreamStatusHandler streamStatusHandler) {
        this.eventSubRepository = eventSubRepository;
        this.broadcasterRepository = broadcasterRepository;
        this.streamSessionRepository = streamSessionRepository;
        this.streamStatusCheckRepository = streamStatusCheckRepository;
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
        this.streamStatusHandler = streamStatusHandler;
    }

    /**
     * Creates EventSub subscriptions for all broadcasters in the database
     */
    public void subscribeAllBroadcasters() {
        List<Broadcaster> broadcasters = broadcasterRepository.findAll();
        
        for (Broadcaster broadcaster : broadcasters) {
            System.out.println("Subscribing to broadcaster " + broadcaster.getId());
            subscribeToStreamerEvents(broadcaster.getId(), broadcaster.getLogin());
        }
    }

    /**
     * Subscribes to stream start and end events for a specific broadcaster
     */
    public void subscribeToStreamerEvents(String broadcasterId, String broadcasterLogin) {
        // Subscription for stream online
        if (!eventSubRepository.existsByTypeAndBroadcasterId(STREAM_ONLINE_TYPE, broadcasterId)) {
            System.out.println("Subscribing to broadcaster online " + broadcasterId);
            createSubscription(STREAM_ONLINE_TYPE, broadcasterId, broadcasterLogin);
        }
        
        // Subscription for stream offline
        if (!eventSubRepository.existsByTypeAndBroadcasterId(STREAM_OFFLINE_TYPE, broadcasterId)) {
            System.out.println("Subscribing to broadcaster offline " + broadcasterId);
            createSubscription(STREAM_OFFLINE_TYPE, broadcasterId, broadcasterLogin);
        }
    }

    /**
     * Creates an EventSub subscription for the specified event type and broadcaster
     */
    private void createSubscription(String type, String broadcasterId, String broadcasterLogin) {
        try {
            // Генерируем секретный ключ для проверки подписи
            String secret = generateSecret();
            
            // Подготавливаем тело запроса
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("type", type);
            requestBody.put("version", VERSION);
            
            Map<String, Object> condition = new HashMap<>();
            condition.put("broadcaster_user_id", broadcasterId);
            requestBody.put("condition", condition);
            
            Map<String, Object> transport = new HashMap<>();
            transport.put("method", "webhook");
            transport.put("callback", webhookUrl);
            transport.put("secret", secret);
            requestBody.put("transport", transport);
            
            // Подготавливаем заголовки
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Client-ID", clientId);
            headers.add("Authorization", "Bearer " + tokenService.getValidAccessToken());
            
            // Отправляем запрос
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    TWITCH_EVENTSUB_API,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            
            // Обрабатываем ответ
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode dataNode = root.get("data").get(0);

                EventSubSubscription subscription = new EventSubSubscription();
                subscription.setId(dataNode.get("id").asText());
                subscription.setType(type);
                subscription.setVersion(VERSION);
                subscription.setStatus(dataNode.get("status").asText());
                subscription.setBroadcasterId(broadcasterId);
                subscription.setBroadcasterLogin(broadcasterLogin);
                subscription.setCreatedAt(LocalDateTime.now());
                subscription.setUpdatedAt(LocalDateTime.now());
                subscription.setSecret(secret);
                subscription.setCallbackUrl(dataNode.get("transport").get("callback").asText());
                subscription.setTransportMethod(dataNode.get("transport").get("method").asText());
                subscription.setCost(dataNode.get("cost").asInt());

                // Сохраняем или обновляем подписку
                eventSubRepository.save(subscription);
                System.out.println("Successfully created or updated subscription " + subscription.getId() + " for " + broadcasterId);
            }

        } catch (Exception e) {
            System.err.println("Error creating EventSub subscription: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Verifies the signature of a request from Twitch
     */
    public boolean verifySignature(String messageId, String timestamp, String signature, String body, String subscriptionId) {
        try {
            Optional<EventSubSubscription> subscriptionOpt = eventSubRepository.findById(subscriptionId);
            if (subscriptionOpt.isEmpty()) {
                return false;
            }
            
            String secret = subscriptionOpt.get().getSecret();
            String message = messageId + timestamp + body;
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((secret + message).getBytes());
            
            String calculatedSignature = "sha256=" + bytesToHex(hash);
            
            return signature.equals(calculatedSignature);
        } catch (Exception e) {
            System.err.println("Error verifying signature: " + e.getMessage());
            return false;
        }
    }

    /**
     * Handles notifications about stream status changes
     */
    public void handleStreamStatusChange(String type, String broadcasterId, boolean isLive) {
        System.out.println("Stream status changed for broadcaster " + broadcasterId + ": " + 
                (type.equals(STREAM_ONLINE_TYPE) ? "online" : "offline"));
        
        try {
            // Создаем новую сессию
            StreamSession session = new StreamSession();
            session.setBroadcasterId(broadcasterId);
            session.setSessionType(isLive ? "ONLINE" : "OFFLINE");
            session.setMessage("Stream " + (isLive ? "started" : "ended") + " (from webhook)");
            
            if (isLive) {
                session.setStartTime(LocalDateTime.now());
            } else {
                session.setEndTime(LocalDateTime.now());
            }
            
            // Сохраняем сессию
            try {
                streamSessionRepository.save(session);
                System.out.println("Stream session saved from webhook: " + session);
            } catch (Exception e) {
                System.err.println("Error saving stream session from webhook: " + e.getMessage());
                e.printStackTrace();
            }
            
            // Обновляем WebSocket для клиентов
            streamStatusHandler.broadcastStreamStatus(broadcasterId, isLive);
            
            
            // Также обновляем информацию о стриме, если он онлайн
            if (isLive) {
                checkStreamStatus(broadcasterId);
            }
        } catch (Exception e) {
            System.err.println("Error handling stream status change: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generates a secret key for signature verification
     */
    private String generateSecret() {
        byte[] randomBytes = new byte[16];
        new SecureRandom().nextBytes(randomBytes);
        return bytesToHex(randomBytes);
    }

    /**
     * Converts bytes to a hexadecimal string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    @Transactional
    public void checkStreamStatus(String broadcasterId) {
        try {
            // Получаем валидный токен
            String token = tokenService.getValidAccessToken();
            
            // Создаем HTTP клиент
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.add("Client-ID", clientId);
            headers.add("Authorization", "Bearer " + token);
            
            // Формируем URL для запроса к Twitch API
            String url = "https://api.twitch.tv/helix/streams?user_id=" + broadcasterId;
            
            // Выполняем запрос
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            
            // Обрабатываем ответ
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode dataNode = root.get("data");
                
                // Проверяем, стримит ли пользователь
                boolean isLive = dataNode.isArray() && dataNode.size() > 0;
                
                // Получаем предыдущий статус из базы данных
                boolean wasLive = false;
                StreamStatusCheck lastCheck = streamStatusCheckRepository
                    .findFirstByBroadcasterIdOrderByCheckTimeDesc(broadcasterId);
                if (lastCheck != null) {
                    wasLive = lastCheck.isLive();
                }
                
                // Если статус изменился, создаем новую сессию
                if (wasLive != isLive) {
                    System.out.println("Stream status changed during periodic check for " + broadcasterId + ": " + 
                        (isLive ? "went online" : "went offline"));
                    
                    // Создаем новую сессию стрима
                    StreamSession session = new StreamSession();
                    session.setBroadcasterId(broadcasterId);
                    session.setSessionType(isLive ? "ONLINE" : "OFFLINE");
                    session.setMessage("Stream " + (isLive ? "started" : "ended") + " (detected by periodic check)");
                    
                    if (isLive) {
                        session.setStartTime(LocalDateTime.now());
                    } else {
                        session.setEndTime(LocalDateTime.now());
                    }
                    
                    try {
                        // Сохраняем сессию
                        streamSessionRepository.save(session);
                        System.out.println("Stream session saved: " + session);
                    } catch (Exception e) {
                        System.err.println("Error saving stream session: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
                // Создаем новую запись о проверке
                StreamStatusCheck statusCheck = new StreamStatusCheck();
                statusCheck.setBroadcasterId(broadcasterId);
                statusCheck.setLive(isLive);
                statusCheck.setCheckTime(LocalDateTime.now());
                
                // Если стрим активен, сохраняем дополнительную информацию
                if (isLive) {
                    JsonNode streamInfo = dataNode.get(0);
                    statusCheck.setStreamTitle(streamInfo.get("title").asText());
                    statusCheck.setGameName(streamInfo.get("game_name").asText());
                    statusCheck.setViewerCount(streamInfo.get("viewer_count").asInt());
                    statusCheck.setThumbnailUrl(streamInfo.get("thumbnail_url").asText());
                    statusCheck.setLanguage(streamInfo.get("language").asText());
                }
                
                // Удаляем старые записи для этого стримера
                streamStatusCheckRepository.deleteAllByBroadcasterId(broadcasterId);
                
                // Сохраняем новую запись
                streamStatusCheckRepository.save(statusCheck);
                
                // Обновляем статус для клиентов через WebSocket
                streamStatusHandler.broadcastStreamStatus(broadcasterId, isLive);
                
                
                System.out.println("Stream status check for " + broadcasterId + ": " + 
                        (isLive ? "online" : "offline"));
            }
        } catch (Exception e) {
            System.err.println("Error checking stream status for " + broadcasterId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Method to check the status of all streamers
    public void checkAllStreamStatuses() {
        List<Broadcaster> broadcasters = broadcasterRepository.findAll();
        for (Broadcaster broadcaster : broadcasters) {
            checkStreamStatus(broadcaster.getId());
        }
    }

    /**
     * Создает тестовую сессию для стримера
     */
    public StreamSession createTestSession(String broadcasterId) {
        StreamSession session = new StreamSession();
        session.setBroadcasterId(broadcasterId);
        session.setSessionType("TEST");
        session.setMessage("Test session created through service");
        session.setStartTime(LocalDateTime.now());
        
        try {
            StreamSession savedSession = streamSessionRepository.save(session);
            System.out.println("Test session saved successfully: " + savedSession);
            return savedSession;
        } catch (Exception e) {
            System.err.println("Error saving test session: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
