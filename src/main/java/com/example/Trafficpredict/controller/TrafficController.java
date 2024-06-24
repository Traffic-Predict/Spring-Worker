package com.example.Trafficpredict.controller;

import com.example.Trafficpredict.dto.GenericResponseWrapper;
import com.example.Trafficpredict.dto.TrafficRequest;
import com.example.Trafficpredict.dto.TrafficResponse;
import com.example.Trafficpredict.service.TrafficService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.example.Trafficpredict.dto.ITSRequest;

import java.util.List;

@RestController
@Slf4j
public class TrafficController {
    @Autowired
    private TrafficService trafficService;

    @PostMapping("/recent")
    public ResponseEntity<GenericResponseWrapper<TrafficResponse>> getRecentTraffic(@RequestBody TrafficRequest request) {
        log.info("Received /recent request in thread: " + Thread.currentThread().getName());
        List<TrafficResponse> responses = trafficService.findRecentTrafficData(request);
        GenericResponseWrapper<TrafficResponse> wrappedResponse = new GenericResponseWrapper<>(responses);
        log.info("/recent request processed successfully in thread: " + Thread.currentThread().getName());
        return ResponseEntity.ok(wrappedResponse);
    }

}
