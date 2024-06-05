package com.example.Trafficpredict.repository;

import com.example.Trafficpredict.model.TrafficData;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrafficDataRepository extends CrudRepository<TrafficData, Long> {

    @Transactional
    @Modifying
    @Query("DELETE FROM TrafficData td WHERE td.date < :cutoff")
    void deleteDataOlderThan(OffsetDateTime cutoff);

    @Query("SELECT t FROM TrafficData t ORDER BY t.date DESC")
    List<TrafficData> findAllOrderByDateDesc();
}
