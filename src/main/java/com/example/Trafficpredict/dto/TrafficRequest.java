package com.example.Trafficpredict.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrafficRequest {
    private Double minX;
    private Double maxX;
    private Double minY;
    private Double maxY;
}
