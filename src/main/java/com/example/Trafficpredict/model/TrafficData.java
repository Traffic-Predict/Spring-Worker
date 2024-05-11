package com.example.Trafficpredict.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "traffic_data")
@Getter
@Setter
public class TrafficData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "link_id")
    private Long linkId;

    @Column(name = "start_node_id")
    private Long startNodeId;

    @Column(name = "end_node_id")
    private Long endNodeId;

    @Column(name = "road_name")
    private String roadName;

    @Column(name = "road_rank")
    private String roadRank;

/*    @Column(name = "geometry")
    private String geometry;*/

    @Column(name = "speed")
    private Double speed;

    @Column(name = "road_status")
    private Integer roadStatus;

    @Column(name = "date")
    private OffsetDateTime date;

    public void setDate(OffsetDateTime date) {
        this.date = date.truncatedTo(ChronoUnit.SECONDS);
    }
}
