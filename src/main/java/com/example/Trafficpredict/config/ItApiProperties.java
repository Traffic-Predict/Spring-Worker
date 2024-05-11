package com.example.Trafficpredict.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "its")
@Data
public class ItApiProperties {
    private String apiKey;
    private String apiUrl;
}
