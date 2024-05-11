package com.example.Trafficpredict.service;

import com.example.Trafficpredict.config.ItApiProperties;
import com.example.Trafficpredict.dto.TrafficRequest;
import com.example.Trafficpredict.dto.TrafficResponse;
import com.example.Trafficpredict.model.TrafficData;
import com.example.Trafficpredict.repository.TrafficDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TrafficService {

    @Autowired
    private ItApiProperties itApiProperties;

    @Autowired
    private TrafficDataRepository trafficDataRepository;

    private static final String DATABASE_URL = "jdbc:sqlite:src/main/resources/daejeon_links_without_geometry.sqlite";
    private static final int EXCLUDE_CITY_LEVEL = 8;

    private TrafficResponse convertData(JSONObject apiResponse) throws SQLException {
        JSONArray items = apiResponse.getJSONObject("body").getJSONArray("items");
        List<TrafficData> dataList = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DATABASE_URL)) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT GEOMETRY, link_id, road_name, road_rank FROM daejeon_link WHERE link_id = ?");

            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                Long linkId = item.optLong("linkId", 0L);  // Default to 0 if not found

                stmt.setLong(1, linkId);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    String roadRank = rs.getString("road_rank");
                    if (roadRank.matches("105|106|107")) {
                        continue;
                    }

                    TrafficData data = new TrafficData();
                    data.setLinkId(linkId);
                    data.setNodeId(item.optLong("startNodeId", 0));
                    data.setRoadName(rs.getString("road_name"));
                    data.setRoadRank(roadRank);
                    /*data.setGeometry(rs.getString("GEOMETRY"));*/
                    data.setSpeed(item.optDouble("speed", 0.0));
                    data.setRoadStatus(determineCongestion(roadRank, item.optDouble("speed", 0.0)));
                    OffsetDateTime nowWithOffset = OffsetDateTime.now(ZoneOffset.of("+09:00"));
                    data.setDate(nowWithOffset);
                    dataList.add(data);
                }
            }
        }
        TrafficResponse trafficResponse = new TrafficResponse();
        trafficResponse.setItems(dataList);
        return trafficResponse;
    }

    public void callApi(TrafficRequest request) throws IOException, SQLException {
        OkHttpClient client = new OkHttpClient();
        HttpUrl.Builder urlBuilder = HttpUrl.parse(itApiProperties.getApiUrl()).newBuilder();
        urlBuilder.addQueryParameter("apiKey", itApiProperties.getApiKey())
                .addQueryParameter("type", "all")
                .addQueryParameter("drcType", "all")
                .addQueryParameter("minX", request.getMinX())
                .addQueryParameter("maxX", request.getMaxX())
                .addQueryParameter("minY", request.getMinY())
                .addQueryParameter("maxY", request.getMaxY())
                .addQueryParameter("getType", "json");

        Request httpRequest = new Request.Builder().url(urlBuilder.build()).build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            String responseBody = response.body().string();
            JSONObject jsonResponse = new JSONObject(responseBody);
            TrafficResponse trafficResponse = convertData(jsonResponse);
            storeData(trafficResponse);
        }
    }


    private void storeData(TrafficResponse trafficResponse) {
        for (TrafficData data : trafficResponse.getItems()) {
            trafficDataRepository.save(data);
        }
    }

    private int determineCongestion(String roadRank, double speed) {
        return switch (roadRank) {
            case "101" -> speed <= 40 ? 3 : speed <= 80 ? 2 : 1;
            case "102" -> speed <= 30 ? 3 : speed <= 60 ? 2 : 1;
            case "103" -> speed <= 20 ? 3 : speed <= 40 ? 2 : 1;
            case "104" -> speed <= 15 ? 3 : speed <= 30 ? 2 : 1;
            default -> 0;
        };
    }
}
