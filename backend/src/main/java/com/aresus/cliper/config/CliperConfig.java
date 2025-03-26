package com.aresus.cliper.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import lombok.Data;

@Configuration
@Component
@Data
public class CliperConfig {

    @Value("${string.CLIENT_ID}")
    private String clientId;

    @Value("${string.CLIENT_SECRET}")
    private String clientSecret;

    @Value("${double.RatelimitLimit}")
    private Double RatelimitLimit;

    @Value("${double.RatelimitRemaining}")  
    private Double RatelimitRemaining;

    @Value("${double.RatelimitReset}")
    private Double RatelimitReset;

}
