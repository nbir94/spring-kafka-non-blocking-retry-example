package com.enbirr.springkafkaretry.configuration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class KafkaCustomProperties {

  @Value("${kafka.topic}")
  private String mainTopic;
  @Value("${kafka.dlq.topic-suffix}")
  private String dlqTopicSuffix;
  @Value("${kafka.retry.topic-suffix}")
  private String retryTopicSuffix;
  @Value("${kafka.retry.interval-ms}")
  private int attemptsIntervalMs;
  @Value("${kafka.retry.attempts-count}")
  private int attemptsMaxCount;
}
