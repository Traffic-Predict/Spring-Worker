package com.example.Trafficpredict.service;

import com.example.Trafficpredict.dto.CachedTrafficData;
import com.example.Trafficpredict.dto.TrafficRequest;
import com.example.Trafficpredict.dto.TrafficResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import com.example.Trafficpredict.repository.TrafficDataRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
public class TrafficService {
    @Autowired
    private TrafficDataRepository trafficDataRepository;

    @Autowired
    private CacheManager cacheManager;

    @Value("${node.db.url}")
    private String NODE_DB_URL;

    @Value("${geometry.db.url}")
    private String GEOMETRY_DB_URL;

    @Transactional(readOnly = true)
    public List<TrafficResponse> findRecentTrafficData(TrafficRequest request) {
        log.info("Processing findRecentTrafficData in thread: " + Thread.currentThread().getName());
        Cache cache = cacheManager.getCache("trafficDataCache");
        List<TrafficResponse> responseList = new ArrayList<>();

        if (cache != null) {
            Map<Object, Object> nativeCache = (Map<Object, Object>) cache.getNativeCache();
            nativeCache.forEach((key, value) -> {
                CachedTrafficData cachedData = (CachedTrafficData) value;
                if (isInBounds(cachedData, request.getMinX(), request.getMaxX(), request.getMinY(), request.getMaxY())) {
                    TrafficResponse data = convertToTrafficResponse(cachedData);
                    if(request.getMapLevel() >= 8 && data.getRoadRank().equals("104")){
                        return;
                    }
                    responseList.add(data);
                }
            });
        }
        log.info("findRecentTrafficData processed successfully in thread: " + Thread.currentThread().getName());
        return responseList;
    }

    private boolean isInBounds(CachedTrafficData data, double minX, double maxX, double minY, double maxY) {
        return (data.getStartX() >= minX && data.getStartX() <= maxX && data.getStartY() >= minY && data.getStartY() <= maxY) ||
                (data.getEndX() >= minX && data.getEndX() <= maxX && data.getEndY() >= minY && data.getEndY() <= maxY);
    }

    private TrafficResponse convertToTrafficResponse(CachedTrafficData cachedData) {
        TrafficResponse response = new TrafficResponse();
        response.setId(cachedData.getId());
        response.setLinkId(cachedData.getLinkId());
        response.setSpeed(cachedData.getSpeed());
        response.setStartNodeId(cachedData.getStartNodeId());
        response.setEndNodeId(cachedData.getEndNodeId());
        response.setRoadName(cachedData.getRoadName());
        response.setRoadRank(cachedData.getRoadRank());
        response.setGeometry(cachedData.getGeometry());
        response.setRoadStatus(cachedData.getRoadStatus());
        response.setDate(cachedData.getDate());
        return response;
    }
}

