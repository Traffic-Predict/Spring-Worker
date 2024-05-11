package com.example.Trafficpredict.dto;

import com.example.Trafficpredict.model.TrafficData;
import lombok.Data;
import java.util.List;

@Data
public class TrafficResponse {
    private List<TrafficData> items;
}
