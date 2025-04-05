package com.aresus.cliper.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aresus.cliper.model.BroadcasterWithStreamInfo;
import com.aresus.cliper.service.SortService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public final class BroadcastersController {

    private final SortService sortService;
    
    @GetMapping("/broadcasters")
    public List<BroadcasterWithStreamInfo> getAllBroadcastersWithStreamInfo() {
        return sortService.getAllBroadcastersWithStreamInfo();
    }
}
