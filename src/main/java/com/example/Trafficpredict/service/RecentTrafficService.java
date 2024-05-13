package com.example.Trafficpredict.service;

import com.example.Trafficpredict.dto.RecentTrafficResponse;
import com.example.Trafficpredict.dto.TrafficRequest;
import com.example.Trafficpredict.dto.TrafficResponse;
import com.example.Trafficpredict.model.TrafficData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.Trafficpredict.repository.TrafficDataRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class RecentTrafficService {
    @Autowired
    private TrafficDataRepository trafficDataRepository;

    @Value("${node.db.url}")
    private String NODE_DB_URL;

    @Value("${geometry.db.url}")
    private String GEOMETRY_DB_URL;

    public List<RecentTrafficResponse> findRecentTrafficDataByNodeIds(List<Long> nodeIds) {
        List<TrafficData> allData = trafficDataRepository.findRecentByNodeIds(nodeIds);
        Map<Long, TrafficData> latestData = new HashMap<>();
        for (TrafficData data : allData) {
            latestData.compute(data.getLinkId(), (key, current) -> current == null ||
                    data.getDate().isAfter(current.getDate()) ? data : current);
        }
        List<TrafficData> filteredData = new ArrayList<>(latestData.values());

        List<RecentTrafficResponse> responseList = new ArrayList<>();

        // geometry 정보를 추가
        try (Connection conn = DriverManager.getConnection(GEOMETRY_DB_URL)) {
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT link_id, geometry FROM daejeon_link_wgs84 WHERE link_id = ?");
            for (TrafficData data : filteredData) {
                pstmt.setLong(1, data.getLinkId());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String geometry = rs.getString("geometry");
                        RecentTrafficResponse response = new RecentTrafficResponse(data, geometry);
                        responseList.add(response);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error fetching geometry data", e);
        }
        return responseList;
    }

    public List<Long> findNodeIdsInArea(double minX, double maxX, double minY, double maxY) {
        String sql = "SELECT node_id FROM daejeon_node_xy WHERE x BETWEEN ? AND ? AND y BETWEEN ? AND ?";
        List<Long> nodeIds = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(NODE_DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, minX);
            pstmt.setDouble(2, maxX);
            pstmt.setDouble(3, minY);
            pstmt.setDouble(4, maxY);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                nodeIds.add(rs.getLong("node_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nodeIds;
    }
}

