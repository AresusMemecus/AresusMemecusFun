package com.aresus.cliper.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.aresus.cliper.config.CliperConfig;
import com.aresus.cliper.model.token.Token;
import com.aresus.cliper.model.token.TokenResponse;
import com.aresus.cliper.model.token.TokenValidate;
import com.aresus.cliper.repository.TokenRepository;

@Service
public class TokenService {

    private static final String TWITCH_VALIDATE_URL = "https://id.twitch.tv/oauth2/validate";
    private static final String TWITCH_TOKEN_URL = "https://id.twitch.tv/oauth2/token";
    private TokenRepository tokenRepository;
    private final CliperConfig config;

    private Token currentToken; 

    @Autowired
    public TokenService(CliperConfig config, TokenRepository tokenRepository) {
        this.config = config;
        this.tokenRepository = tokenRepository;
    }


    public String getValidAccessToken() {
        if (currentToken == null || !validateToken(currentToken.getAccessToken())) {
            System.out.println("Token is missing or invalid. Fetching a new one...");
            currentToken = getOrRequestValidToken();
        } else {
            System.out.println("Using existing token.");
        }
    
        return currentToken.getAccessToken();
    }
    

    public boolean validateToken(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "OAuth " + accessToken);
    
        Token token = tokenRepository.findByAccessToken(accessToken);
    
        if (token == null) {
            System.out.println("Token not found in database.");
            return false; // Токен отсутствует в базе, запросить новый
        }
    
        if (token.getLastValidatedAt() != null) {
            Duration durationSinceLastValidation = Duration.between(token.getLastValidatedAt(), LocalDateTime.now());
            if (durationSinceLastValidation.toMinutes() < 60) { 
                System.out.println("Token validation skipped (cooldown active). Using token.");
                return true; // Используем токен, даже если он не валидировался
            }
        }
    
        HttpEntity<String> entity = new HttpEntity<>(headers);
    
        try {
            ResponseEntity<TokenValidate> response = restTemplate.exchange(
                    TWITCH_VALIDATE_URL, HttpMethod.GET, entity, TokenValidate.class);
    
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                TokenValidate validationResponse = response.getBody();
                if (validationResponse == null) return false;
                int expiresIn = validationResponse.getExpiresIn();
    
                token.setExpiresIn(expiresIn);
                token.setLastValidatedAt(LocalDateTime.now()); 
                tokenRepository.save(token);
    
                System.out.println("Token is valid and updated.");
                return true;
            }
        } catch (Exception e) {
            System.out.println("Token validation failed: " + e.getMessage());
        }
    
        return false; // Если валидация не прошла, токен недействителен
    }
    

    public Token requestNewToken() {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", config.getClientId());
        body.add("client_secret", config.getClientSecret());
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                    TWITCH_TOKEN_URL,
                    request,
                    TokenResponse.class
            );

            System.out.println("Response status: " + response.getStatusCode());
            System.out.println("Response body: " + response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                TokenResponse tokenResponse = response.getBody();
                if (tokenResponse.getAccessToken() != null) {
                    Token newToken = new Token(
                            tokenResponse.getAccessToken(),
                            tokenResponse.getExpiresIn(),
                            tokenResponse.getTokenType()
                    );
                    tokenRepository.save(newToken);
                    return newToken;
                } else {
                    throw new RuntimeException("Access token is null in the response.");
                }
            } else {
                throw new RuntimeException("Failed to fetch token. HTTP Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error during token request: " + e.getMessage(), e);
        }
    }

    public Token getOrRequestValidToken() {
        Token latestToken = tokenRepository.findFirstByOrderByIdDesc();

        if (latestToken == null) {
            System.out.println("No token found in database. Requesting a new one.");
            Token newToken = requestNewToken();

            if (validateToken(newToken.getAccessToken())) {
                System.out.println("Newly fetched token is valid.");
                return newToken;
            } else {
                throw new RuntimeException("Newly fetched token is invalid.");
            }
        }

        if (!validateToken(latestToken.getAccessToken())) {
            System.out.println("Latest token is invalid. Requesting a new one.");
            Token newToken = requestNewToken();

            if (validateToken(newToken.getAccessToken())) {
                System.out.println("Newly fetched token is valid.");
                return newToken;
            } else {
                throw new RuntimeException("Newly fetched token is invalid.");
            }
        }

        System.out.println("Returning valid token from database.");
        return latestToken;
    }

}
