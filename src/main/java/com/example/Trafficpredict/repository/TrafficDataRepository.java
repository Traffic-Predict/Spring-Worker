package com.example.Trafficpredict.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.Trafficpredict.model.TrafficData;

public interface TrafficDataRepository extends JpaRepository<TrafficData, Long> {
}
