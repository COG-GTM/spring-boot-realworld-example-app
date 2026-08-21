package io.spring.infrastructure.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PendoTrackService {
  private static final Logger logger = LoggerFactory.getLogger(PendoTrackService.class);
  private final RestTemplate restTemplate;
  private final String integrationKey;
  private final String trackUrl;

  public PendoTrackService(
      @Value("${pendo.integration-key:}") String integrationKey,
      @Value("${pendo.data-host:https://data.pendo.io}") String dataHost) {
    this.restTemplate = new RestTemplate();
    this.integrationKey = integrationKey;
    this.trackUrl = dataHost + "/data/track";
  }

  public void track(String event, String visitorId, Map<String, Object> properties) {
    if (integrationKey == null || integrationKey.isEmpty()) {
      logger.debug("Pendo integration key not configured, skipping track event: {}", event);
      return;
    }

    CompletableFuture.runAsync(
        () -> {
          try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-pendo-integration-key", integrationKey);

            Map<String, Object> body = new HashMap<>();
            body.put("type", "track");
            body.put("event", event);
            body.put("visitorId", visitorId != null ? visitorId : "system");
            body.put("accountId", "system");
            body.put("timestamp", System.currentTimeMillis());
            if (properties != null && !properties.isEmpty()) {
              body.put("properties", properties);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(trackUrl, request, String.class);
          } catch (Exception e) {
            logger.warn("Failed to send Pendo track event: {}", event, e);
          }
        });
  }
}
