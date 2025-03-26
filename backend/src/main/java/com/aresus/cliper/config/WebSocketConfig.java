package com.aresus.cliper.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.aresus.cliper.websocket.ClipsBroadcasterHandler;
import com.aresus.cliper.websocket.ClipsTimerHandler;
import com.aresus.cliper.websocket.StreamStatusHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ClipsTimerHandler clipsTimerHandler;
    private final ClipsBroadcasterHandler clipsBroadcasterHandler;
    private final StreamStatusHandler streamStatusHandler;

    public WebSocketConfig(
            ClipsTimerHandler clipsTimerHandler,
            ClipsBroadcasterHandler clipsBroadcasterHandler,
            StreamStatusHandler streamStatusHandler) {
        this.clipsTimerHandler = clipsTimerHandler;
        this.clipsBroadcasterHandler = clipsBroadcasterHandler;
        this.streamStatusHandler = streamStatusHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(clipsTimerHandler, "/ws/clips/status/timer")
                .setAllowedOrigins("*");
        registry.addHandler(clipsBroadcasterHandler, "/ws/clips/status/broadcaster")
                .setAllowedOrigins("*");
        registry.addHandler(streamStatusHandler, "/ws/stream/status")
                .setAllowedOrigins("*");
    }
}

