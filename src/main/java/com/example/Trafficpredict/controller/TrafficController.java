package com.example.Trafficpredict.controller;

import com.example.Trafficpredict.dto.GenericResponseWrapper;
import com.example.Trafficpredict.dto.TrafficRequest;
import com.example.Trafficpredict.dto.TrafficResponse;
import com.example.Trafficpredict.service.TrafficService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.example.Trafficpredict.dto.ITSRequest;

import java.util.List;

@RestController
public class TrafficController {
    @Autowired
    private TrafficService trafficService;

    @PostMapping("/recent")
    public ResponseEntity<GenericResponseWrapper<TrafficResponse>> getRecentTraffic(@RequestBody TrafficRequest request) {
        List<Long> nodeIds = trafficService.findNodeIdsInArea(request.getMinX(), request.getMaxX(), request.getMinY(), request.getMaxY());
        List<TrafficResponse> responses = trafficService.findRecentTrafficDataByNodeIds(request, nodeIds);
        GenericResponseWrapper<TrafficResponse> wrappedResponse = new GenericResponseWrapper<>(responses);
        return ResponseEntity.ok(wrappedResponse);
    }
}
