package com.example.Trafficpredict.dto;

import com.example.Trafficpredict.model.TrafficData;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class RecentTrafficResponse {
    private Long id;
    private Long linkId;
    private Long startNodeId;
    private Long endNodeId;
    private String roadName;
    private String roadRank;
    private Double speed;
    private Integer roadStatus;
    private String date;
    private String geometry;  // 추가된 geometry 필드

    public RecentTrafficResponse(TrafficData data, String geometry) {
        this.id = data.getId();
        this.linkId = data.getLinkId();
        this.startNodeId = data.getStartNodeId();
        this.endNodeId = data.getEndNodeId();
        this.roadName = data.getRoadName();
        this.roadRank = data.getRoadRank();
        this.speed = data.getSpeed();
        this.roadStatus = data.getRoadStatus();
        this.date = data.getDate().toString();
        this.geometry = geometry;
    }
}
