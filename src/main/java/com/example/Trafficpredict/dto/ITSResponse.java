package com.example.Trafficpredict.dto;

import com.example.Trafficpredict.model.TrafficData;
import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
@Getter
public class ITSResponse {
    private List<TrafficData> items;
}
