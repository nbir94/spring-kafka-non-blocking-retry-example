package com.enbirr.springkafkaretry.consumer;

import static com.enbirr.springkafkaretry.util.KafkaUtils.getRecordDetails;

import com.enbirr.springkafkaretry.configuration.KafkaCustomProperties;
import com.enbirr.springkafkaretry.constants.LogAndExceptionMessages;
import com.enbirr.springkafkaretry.exception.ProcessingException;
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
  private final KafkaCustomProperties kafkaProperties;

  @KafkaListener(topics = "#{'${kafka.topic}'}")
  public void consumeMessage(ConsumerRecord<String, String> consumerRecord) {
    String recordDetails = getRecordDetails(consumerRecord, kafkaProperties.getAttemptsMaxCount());
    log.info(LogAndExceptionMessages.MESSAGE_RECEIVED, recordDetails, consumerRecord.value());

    String processedMsg = tryToProcess(consumerRecord.value(), recordDetails);
    log.info(LogAndExceptionMessages.MESSAGE_PROCESSED, recordDetails, processedMsg);
  }

  private String tryToProcess(String message, String recordDetails) {
    try {
      return processingService.truncateMessageInRiskyWay(message);
    } catch (ProcessingException ex) {
      log.error(LogAndExceptionMessages.MESSAGE_FAILED_TO_PROCESS, recordDetails, ex);
      throw ex;
    }
  }
}
