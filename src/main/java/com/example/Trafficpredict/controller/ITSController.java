package com.example.Trafficpredict.controller;

import com.example.Trafficpredict.service.ITSService;
import com.example.Trafficpredict.dto.ITSRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ITSController {

    @Autowired
    private ITSService ITSService;

/*    @PostMapping("/main")
    public ResponseEntity<?> handleTrafficData(@RequestBody ITSRequest request) {
        try {
            ITSService.callApi(request); // 데이터 처리 및 저장
            return ResponseEntity.ok("Data processed and stored successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("API 요청 실패: " + e.getMessage());
        }
    }*/
}
