package com.example.Trafficpredict.controller;

import com.example.Trafficpredict.dto.GenericResponseWrapper;
import com.example.Trafficpredict.dto.RecentTrafficResponse;
import com.example.Trafficpredict.dto.TrafficResponse;
import com.example.Trafficpredict.model.TrafficData;
import com.example.Trafficpredict.service.RecentTrafficService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.example.Trafficpredict.dto.TrafficRequest;

import java.util.List;

@RestController
public class RecentTrafficController {
    @Autowired
    private RecentTrafficService recentTrafficService;

    @PostMapping("/recent")
    public ResponseEntity<GenericResponseWrapper<RecentTrafficResponse>> getRecentTraffic(@RequestBody TrafficRequest request) {
        List<Long> nodeIds = recentTrafficService.findNodeIdsInArea(request.getMinX(), request.getMaxX(), request.getMinY(), request.getMaxY());
        List<RecentTrafficResponse> responses = recentTrafficService.findRecentTrafficDataByNodeIds(nodeIds);
        GenericResponseWrapper<RecentTrafficResponse> wrappedResponse = new GenericResponseWrapper<>(responses);
        return ResponseEntity.ok(wrappedResponse);
    }
}
