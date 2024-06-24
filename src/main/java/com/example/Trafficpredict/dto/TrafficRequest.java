package com.example.Trafficpredict.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TrafficRequest {
    private Double minX;
    private Double maxX;
    private Double minY;
    private Double maxY;
    @Builder.Default
    private Integer zoom = 1;
}
