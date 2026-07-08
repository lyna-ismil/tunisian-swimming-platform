package com.ftn.platform.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "ml.service")
@Data
@Slf4j
public class MLServiceConfig {

    private String baseUrl = "http://localhost:8000";
    private int timeout = 10000;
    private int maxInMemorySize = 16 * 1024 * 1024;

    @Bean(name = "mlWebClient")
    public WebClient mlWebClient() {
        ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(config -> {
                config.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(mapper));
                config.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(mapper));
                config.defaultCodecs().maxInMemorySize(maxInMemorySize);
            })
            .build();

        HttpClient httpClient = HttpClient.create()
            .responseTimeout(Duration.ofMillis(timeout));

        log.info("ML WebClient configured: baseUrl={}, timeout={}ms", baseUrl, timeout);

        return WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(strategies)
            .build();
    }
}
