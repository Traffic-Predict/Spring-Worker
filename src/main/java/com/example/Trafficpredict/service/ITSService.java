package com.example.Trafficpredict.service;

import com.example.Trafficpredict.config.ItApiProperties;
import com.example.Trafficpredict.dto.ITSRequest;
import com.example.Trafficpredict.dto.ITSResponse;
import com.example.Trafficpredict.model.TrafficData;
import com.example.Trafficpredict.repository.TrafficDataRepository;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONObject;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.sql.*;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
@Slf4j
@Configuration
public class ITSService {

    @Autowired
    private ItApiProperties itApiProperties;
    @Autowired
    private TrafficDataRepository trafficDataRepository;
    @Value("${geometry.db.url}")
    private String GEOMETRY_DB_URL;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${batch.size}")
    private int batchSize;

    // 대전 범위
    private static final Double MIN_X = 127.269182;
    private static final Double MAX_X = 127.530568;
    private static final Double MIN_Y = 36.192478;
    private static final Double MAX_Y = 36.497312;

    // 10분 간격으로 실행
    @Scheduled(fixedRate = 600000)
    @Transactional
    @Async("taskExecutor")
    public void fetchAndStoreTrafficData() {
        try {
            ITSRequest request = new ITSRequest(MIN_X, MAX_X, MIN_Y, MAX_Y);
            callApi(request);
            log.info("Scheduled API call executed successfully.");
            cleanOldData();
        } catch (Exception e) {
            log.error("Error during scheduled API call: ", e);
        }
    }

    @Transactional
    public void cleanOldData() {
        // 1시간 이전 데이터 삭제
        OffsetDateTime oneHourAgo = OffsetDateTime.now().minusHours(1);
        long countBefore = trafficDataRepository.count();
        trafficDataRepository.deleteDataOlderThan(oneHourAgo);
        long countAfter = trafficDataRepository.count();
        log.info("Old data cleaned up successfully. Before: {}, After: {}", countBefore, countAfter);
    }

    @Transactional
    public ITSResponse convertData(JSONObject apiResponse) throws SQLException {
        JSONArray items = apiResponse.getJSONObject("body").getJSONArray("items");
        List<TrafficData> dataList = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(GEOMETRY_DB_URL)) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT GEOMETRY, link_id, road_name, road_rank FROM daejeon_link_wgs84 WHERE link_id = ?");

            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                Long linkId = item.optLong("linkId", 0L);

                stmt.setLong(1, linkId);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    String roadRank = rs.getString("road_rank");
                    if (roadRank.matches("105|106|107")) {
                        continue;
                    }

                    TrafficData data = new TrafficData();
                    data.setLinkId(linkId);
                    data.setStartNodeId(item.optLong("startNodeId", 0));
                    data.setEndNodeId(item.optLong("endNodeId", 0));
                    data.setRoadName(rs.getString("road_name"));
                    data.setRoadRank(roadRank);
                    /*data.setGeometry(rs.getString("GEOMETRY"));*/
                    data.setSpeed(item.optDouble("speed", 0.0));
                    data.setRoadStatus(determineCongestion(roadRank, item.optDouble("speed", 0.0)));
                    data.setDate(OffsetDateTime.now(ZoneId.of("Asia/Seoul")));
                    dataList.add(data);
                }
            }
        }
        ITSResponse ITSResponse = new ITSResponse();
        ITSResponse.setItems(dataList);
        return ITSResponse;
    }

    @Transactional
    public void callApi(ITSRequest request) throws IOException, SQLException {
        OkHttpClient client = new OkHttpClient();
        HttpUrl.Builder urlBuilder = HttpUrl.parse(itApiProperties.getApiUrl()).newBuilder();
        urlBuilder.addQueryParameter("apiKey", itApiProperties.getApiKey())
                .addQueryParameter("type", "all")
                .addQueryParameter("drcType", "all")
                .addQueryParameter("minX", String.valueOf(request.getMinX()))
                .addQueryParameter("maxX", String.valueOf(request.getMaxX()))
                .addQueryParameter("minY", String.valueOf(request.getMinY()))
                .addQueryParameter("maxY", String.valueOf(request.getMaxY()))
                .addQueryParameter("getType", "json");

        Request httpRequest = new Request.Builder().url(urlBuilder.build()).build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            String responseBody = response.body().string();
            JSONObject jsonResponse = new JSONObject(responseBody);
            ITSResponse ITSResponse = convertData(jsonResponse);
            storeData(ITSResponse);
        }
    }

    @Transactional
    public void storeData(ITSResponse ITSResponse) {
        List<TrafficData> items = ITSResponse.getItems();

        log.info("Batch processing started. Total items to process: {}", items.size());

        for (int i = 0; i < items.size(); i += batchSize) {
            int end = Math.min(items.size(), i + batchSize);
            List<TrafficData> batchList = items.subList(i, end);
            trafficDataRepository.saveAll(batchList);
            entityManager.flush();
            entityManager.clear();
            log.info("Processed batch from index {} to {}. Batch size: {}", i, end - 1, batchList.size());
        }

        log.info("Batch processing completed. Total items processed: {}", items.size());
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
