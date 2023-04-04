package com.enbirr.springkafkaretry.consumer;

import com.enbirr.springkafkaretry.configuration.KafkaCustomProperties;
import com.enbirr.springkafkaretry.service.ProcessingService;
import com.enbirr.springkafkaretry.util.KafkaUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {

  private final ProcessingService processingService;
  private final KafkaCustomProperties kafkaProperties;

  @KafkaListener(topics = "#{'${kafka.topic}'}")
  public void consumeMessage(ConsumerRecord<String, String> consumerRecord) {
    String recordDetails = KafkaUtils.getRecordDetails(consumerRecord,
        kafkaProperties.getAttemptsMaxCount());
    log.info("Message received ({}). Payload: \"{}\"", recordDetails, consumerRecord.value());

    String processedMsg = processingService.truncateMessageInRiskyWay(consumerRecord.value());
    log.info("Message successfully processed ({}). Result: \"{}\"", recordDetails, processedMsg);
  }
}
