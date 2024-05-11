package com.example.Trafficpredict.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrafficRequest {
    private String minX;
    private String maxX;
    private String minY;
    private String maxY;
}
