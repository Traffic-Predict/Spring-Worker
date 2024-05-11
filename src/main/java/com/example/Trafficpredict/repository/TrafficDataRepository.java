package com.example.Trafficpredict.repository;

import com.example.Trafficpredict.model.TrafficData;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface TrafficDataRepository extends CrudRepository<TrafficData, Long> {

    @Transactional
    @Modifying
    @Query("DELETE FROM TrafficData td WHERE td.date < :cutoff")
    void deleteDataOlderThan(OffsetDateTime cutoff);
    @Query("SELECT td FROM TrafficData td WHERE td.startNodeId IN :nodeIds OR td.endNodeId IN :nodeIds ORDER BY td.date DESC")
    List<TrafficData> findRecentByNodeIds(@Param("nodeIds") List<Long> nodeIds);
}
