package com.example.Trafficpredict.controller;

import com.example.Trafficpredict.service.TrafficService;
import com.example.Trafficpredict.dto.TrafficRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TrafficController {

    @Autowired
    private TrafficService trafficService;

    @PostMapping("/main")
    public ResponseEntity<?> handleTrafficData(@RequestBody TrafficRequest request) {
        try {
            trafficService.callApi(request); // 데이터 처리 및 저장
            return ResponseEntity.ok("Data processed and stored successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("API 요청 실패: " + e.getMessage());
        }
    }
}
