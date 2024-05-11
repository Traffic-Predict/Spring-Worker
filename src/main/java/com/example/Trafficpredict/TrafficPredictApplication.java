package com.example.Trafficpredict;

import com.example.Trafficpredict.config.ItApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableConfigurationProperties(ItApiProperties.class)
@EnableJpaRepositories(basePackages = "com.example.Trafficpredict.repository")
public class TrafficPredictApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrafficPredictApplication.class, args);
	}
}
