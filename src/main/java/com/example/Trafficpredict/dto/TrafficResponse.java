package com.example.Trafficpredict.dto;

import com.example.Trafficpredict.model.TrafficData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
public class TrafficResponse {
    private Long id;
    private String linkId;
    private String startNodeId;
    private String endNodeId;
    private String roadName;
    private String roadRank;
    private Double speed;
    private Integer roadStatus;
    private String date;
    private String geometry;
}
