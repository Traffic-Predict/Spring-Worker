package com.example.Trafficpredict.service;

import com.example.Trafficpredict.config.ItApiProperties;
import com.example.Trafficpredict.dto.CachedTrafficData;
import com.example.Trafficpredict.dto.ITSRequest;
import com.example.Trafficpredict.dto.ITSResponse;
import com.example.Trafficpredict.dto.TrafficResponse;
import com.example.Trafficpredict.model.TrafficData;
import com.example.Trafficpredict.repository.TrafficDataRepository;

import com.mysql.cj.protocol.Resultset;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.*;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

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

    @Value("${node.db.url}")
    private String NODE_DB_URL;

    @Value("${geometry.db.url}")
    private String GEOMETRY_DB_URL;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${batch.size}")
    private int batchSize;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RestTemplate restTemplate;

    // 대전 범위
    private static final Double MIN_X = 127.269182;
    private static final Double MAX_X = 127.530568;
    private static final Double MIN_Y = 36.192478;
    private static final Double MAX_Y = 36.497312;

    // 5분 간격으로 실행
    @Scheduled(fixedRate = 300000)
    @Transactional
    @Async("taskExecutor")
    public void fetchAndStoreTrafficData() {
        try {
            log.info("fetchAndStoreTrafficData started in thread: " + Thread.currentThread().getName());
            ITSRequest request = new ITSRequest(MIN_X, MAX_X, MIN_Y, MAX_Y);
            callApi(request);
            log.info("Scheduled API call executed successfully.");
            updateCache();
            //processTrafficData();
            cleanOldData();

        } catch (Exception e) {
            log.error("Error during scheduled API call: ", e);
        }
    }

    @Transactional
    public void cleanOldData() {
        // 24시간 이전 데이터 삭제
        OffsetDateTime nHourAgo = OffsetDateTime.now().minusHours(24);
        long countBefore = trafficDataRepository.count();
        trafficDataRepository.deleteDataOlderThan(nHourAgo);
        long countAfter = trafficDataRepository.count();
        log.info("Old data cleaned up successfully. Before: {}, After: {}", countBefore, countAfter);
    }

    @Transactional
    public ITSResponse convertData(JSONObject apiResponse) throws SQLException {
        JSONArray items = apiResponse.getJSONObject("body").getJSONArray("items");
        List<TrafficData> dataList = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(GEOMETRY_DB_URL)) {
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                String linkId = item.optString("linkId", "0");

                String roadRank = getRoadRankByLinkId(conn, linkId);

                if (!roadRank.matches("101|102|103|104|105")) {
                    continue;
                }

                TrafficData data = new TrafficData();
                data.setLinkId(linkId);
                data.setSpeed(item.optDouble("speed", 0.0));
                data.setDate(OffsetDateTime.now(ZoneId.of("Asia/Seoul")));
                dataList.add(data);
            }
        }

        ITSResponse ITSResponse = new ITSResponse();
        ITSResponse.setItems(dataList);
        return ITSResponse;
    }

    private String getRoadRankByLinkId(Connection conn, String linkId) throws SQLException {
        String roadRank = "0";
        String query = "SELECT road_rank FROM daejeon_link_wgs84 WHERE link_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, linkId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    roadRank = rs.getString("road_rank");
                }
            }
        }
        return roadRank;
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
    public void updateCache() {
        List<TrafficData> recentData = getRecentTrafficData();
        Cache cache = cacheManager.getCache("trafficDataCache");

        if (cache != null) {
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (TrafficData data : recentData) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        CachedTrafficData cachedData = fetchInfoFromSqlite(data);
                        cache.put(data.getLinkId(), cachedData);
                    } catch (SQLException e) {
                        log.error("Error updating cache: ", e);
                    }
                });
                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            log.info("Cache updated with the most recent traffic data.");
        }
    }

    @Transactional
    public void storeData(ITSResponse ITSResponse) {
        List<TrafficData> items = ITSResponse.getItems();

        log.info("Batch processing started in thread: " + Thread.currentThread().getName() + ". Total items to process: " + items.size());

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
            case "105" -> speed <= 30 ? 3 : speed <= 60 ? 2 : 1;
            default -> 0;
        };
    }

    private CachedTrafficData fetchInfoFromSqlite(TrafficData data) throws SQLException {
        CachedTrafficData cachedData = new CachedTrafficData();
        try(Connection conn = DriverManager.getConnection(GEOMETRY_DB_URL)){
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT road_name, road_rank, f_node, t_node, GEOMETRY FROM daejeon_link_wgs84 WHERE link_id = ?"
            );
            stmt.setString(1, data.getLinkId());
            ResultSet rs = stmt.executeQuery();

            if(rs.next()){
                String roadRank = rs.getString("road_rank");
                Double speed = data.getSpeed();
                String startNodeId = rs.getString("f_node");
                String endNodeId = rs.getString("t_node");

                cachedData.setId(data.getId());
                cachedData.setLinkId(data.getLinkId());
                cachedData.setSpeed(speed);
                cachedData.setStartNodeId(startNodeId);
                cachedData.setEndNodeId(endNodeId);
                cachedData.setRoadName(rs.getString("road_name"));
                cachedData.setRoadRank(rs.getString("road_rank"));
                cachedData.setGeometry(rs.getString("GEOMETRY"));

                double[] startCoords = getNodeCoordinates(startNodeId);
                cachedData.setStartX(startCoords[0]);
                cachedData.setStartY(startCoords[1]);

                double[] endCoords = getNodeCoordinates(endNodeId);
                cachedData.setEndX(endCoords[0]);
                cachedData.setEndY(endCoords[1]);

                cachedData.setRoadStatus(determineCongestion(roadRank, speed));

                OffsetDateTime koreaTime = data.getDate().withOffsetSameInstant(ZoneOffset.ofHours(9));
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
                cachedData.setDate(koreaTime.format(formatter));
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }

        return cachedData;
    }

    private double[] getNodeCoordinates(String nodeId) throws SQLException {
        double[] coordinates = new double[2];
        String query = "SELECT x, y FROM daejeon_node_xy WHERE node_id = ?";
        try (Connection conn = DriverManager.getConnection(NODE_DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, nodeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    coordinates[0] = rs.getDouble("x");
                    coordinates[1] = rs.getDouble("y");
                }
            }
        }
        return coordinates;
    }

    // 실시간 데이터를 기반으로 AI 예측 수행
    @Transactional
    public void processTrafficData() {
        // DB에서 가장 최근의 트래픽 데이터를 가져옴
        List<TrafficData> recentTrafficData = getRecentTrafficData();
        String csvData = convertToCsv(recentTrafficData);

        log.info("Converted CSV Data:\n" + csvData);

        // CSV 데이터를 AI 예측 서버로 전송
        String predictionEndpoint = "http://localhost:5000/predict";
        //String predictionResponse = sendCsvForPrediction(csvData, predictionEndpoint);

    }

    private List<TrafficData> getRecentTrafficData() {
        List<TrafficData> allData = trafficDataRepository.findAllOrderByDateDesc();
        Set<String> processedLinkIds = new HashSet<>();
        List<TrafficData> recentData = new ArrayList<>();

        for (TrafficData data : allData) {
            if (!processedLinkIds.contains(data.getLinkId())) {
                recentData.add(data);
                processedLinkIds.add(data.getLinkId());
            } else {
                break;
            }
        }
        return recentData;
    }

    private String convertToCsv(List<TrafficData> trafficDataList) {
        try {
            StringWriter writer = new StringWriter();
            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("linkId","speed", "date"));

            for (TrafficData data : trafficDataList) {
                csvPrinter.printRecord(data.getLinkId(), data.getSpeed(), data.getDate());
            }

            csvPrinter.flush();
            return writer.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String sendCsvForPrediction(String csvData, String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        HttpEntity<String> requestEntity = new HttpEntity<>(csvData, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        return response.getBody();
    }

}
