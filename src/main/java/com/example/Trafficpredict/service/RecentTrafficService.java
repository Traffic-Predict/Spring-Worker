package com.example.Trafficpredict.service;

import com.example.Trafficpredict.dto.RecentTrafficResponse;
import com.example.Trafficpredict.dto.TrafficRequest;
import com.example.Trafficpredict.dto.TrafficResponse;
import com.example.Trafficpredict.model.TrafficData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.Trafficpredict.repository.TrafficDataRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class RecentTrafficService {
    @Autowired
    private TrafficDataRepository trafficDataRepository;

    private static final String NODE_DB_URL = "jdbc:sqlite:src/main/resources/daejeon_node_xy.sqlite";
    private static final String GEOMETRY_DB_URL = "jdbc:sqlite:src/main/resources/daejeon_links_without_geometry.sqlite";

    public List<RecentTrafficResponse> findRecentTrafficDataByNodeIds(List<Long> nodeIds) {
        List<TrafficData> dataList = trafficDataRepository.findRecentByNodeIds(nodeIds);
        List<RecentTrafficResponse> responseList = new ArrayList<>();

        // geometry 정보를 추가
        try (Connection conn = DriverManager.getConnection(GEOMETRY_DB_URL)) {
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT link_id, geometry FROM daejeon_link WHERE link_id = ?");
            for (TrafficData data : dataList) {
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

