package com.example.Trafficpredict.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.OffsetDateTime;

@Entity
@Table(name = "traffic_data")
@Getter
@Setter
public class TrafficData {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "traffic_data_seq")
    @SequenceGenerator(name = "traffic_data_seq", sequenceName = "traffic_data_seq", allocationSize = 200)
    private Long id;

    @Column(name = "link_id")
    private String linkId;

/*    @Column(name = "start_node_id")
    private Long startNodeId;

    @Column(name = "end_node_id")
    private Long endNodeId;

    @Column(name = "road_name")
    private String roadName;

    @Column(name = "road_rank")
    private String roadRank;*/

    @Column(name = "speed")
    private Double speed;

/*    @Column(name = "road_status")
    private Integer roadStatus;*/

    @Column(name = "date", columnDefinition = "TIMESTAMP")
    private OffsetDateTime date;


    //date 필드를 5분 단위로 끊어서 저장
    @PrePersist
    public void onPrePersist() {
        this.date = truncateToFiveMinutes(this.date);
    }

    private OffsetDateTime truncateToFiveMinutes(OffsetDateTime dateTime) {
        int minute = dateTime.getMinute();
        int truncatedMinute = (minute / 5) * 5;
        return dateTime.withMinute(truncatedMinute).withSecond(0).withNano(0);
    }

}
