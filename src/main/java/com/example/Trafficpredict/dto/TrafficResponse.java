package com.example.Trafficpredict.dto;

import com.example.Trafficpredict.model.TrafficData;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
public class TrafficResponse {
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

    public TrafficResponse(TrafficData data, String geometry) {
        this.id = data.getId();
        this.linkId = data.getLinkId();
        this.startNodeId = data.getStartNodeId();
        this.endNodeId = data.getEndNodeId();
        this.roadName = data.getRoadName();
        this.roadRank = data.getRoadRank();
        this.speed = data.getSpeed();
        this.roadStatus = data.getRoadStatus();
        this.geometry = geometry;

        OffsetDateTime koreaTime = data.getDate().withOffsetSameInstant(ZoneOffset.ofHours(9));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
        this.date = koreaTime.format(formatter);
    }
}
