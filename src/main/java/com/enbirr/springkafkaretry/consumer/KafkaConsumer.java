package com.enbirr.springkafkaretry.consumer;

import com.enbirr.springkafkaretry.service.ProcessingService;
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

  @KafkaListener(topics = "#{'${kafka.mainTopic}'}")
  public void consumeMessage(ConsumerRecord<String, String> consumerRecord) {
    String recordDetails = getRecordDetails(consumerRecord);
    log.info("Message received ({}). Payload: \"{}\"", recordDetails, consumerRecord.value());

    String processedMsg = processingService.truncateMessage(consumerRecord.value());
    log.info("Message successfully processed ({}). Result: \"{}\"", recordDetails, processedMsg);
  }

  private static String getRecordDetails(ConsumerRecord<String, String> consumerRecord) {
    return String.format("topic: '%s', partition: %d, offset: %d",
        consumerRecord.topic(),
        consumerRecord.partition(),
        consumerRecord.offset()
    );
  }
}
