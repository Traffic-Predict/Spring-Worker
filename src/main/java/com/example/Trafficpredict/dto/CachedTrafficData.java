package com.example.Trafficpredict.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CachedTrafficData {
    private Long id;
    private String linkId;
    private String startNodeId;
    private String endNodeId;
    private double speed;
    private double startX;
    private double startY;
    private double endX;
    private double endY;
    private String roadName;
    private String roadRank;
    private String geometry;
    private int roadStatus;
    private String date;
}