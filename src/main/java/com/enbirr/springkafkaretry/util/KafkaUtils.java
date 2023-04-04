package com.enbirr.springkafkaretry.util;

import static org.springframework.kafka.retrytopic.RetryTopicHeaders.DEFAULT_HEADER_ATTEMPTS;
import static org.springframework.kafka.support.KafkaHeaders.ORIGINAL_OFFSET;
import static org.springframework.kafka.support.KafkaHeaders.ORIGINAL_PARTITION;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.nio.ByteBuffer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KafkaUtils {

  public static String getRecordDetails(
      ConsumerRecord<String, String> consumerRecord,
      int maxAttemptsCount
  ) {
    Integer originalPartition = getIntFromHeader(consumerRecord, ORIGINAL_PARTITION);
    Long originalOffset = getLongFromHeader(consumerRecord, ORIGINAL_OFFSET);

    StringBuilder strBuilder = new StringBuilder();
    strBuilder.append(String.format("topic: %s, partition: %d, offset: %d",
        consumerRecord.topic(), consumerRecord.partition(), consumerRecord.offset()));

    if (originalPartition != null && originalOffset != null) {
      strBuilder.append(String.format(", original partition: %d, original offset: %d",
          originalPartition, originalOffset));
    }

    Integer attemptNumber = getIntFromHeader(consumerRecord, DEFAULT_HEADER_ATTEMPTS);
    String attemptAppendix = String.format(", attempt number: %d of %d",
        attemptNumber == null ? 1 : attemptNumber,
        maxAttemptsCount);
    strBuilder.append(attemptAppendix);

    return strBuilder.toString();
  }

  private static Integer getIntFromHeader(ConsumerRecord<?, ?> consumerRecord, String headerKey) {
    var header = consumerRecord.headers().lastHeader(headerKey);
    if (header != null) {
      return ByteBuffer.wrap(header.value()).getInt();
    } else {
      return null;
    }
  }

  private static Long getLongFromHeader(ConsumerRecord<?, ?> consumerRecord, String headerKey) {
    var header = consumerRecord.headers().lastHeader(headerKey);
    if (header != null) {
      return ByteBuffer.wrap(header.value()).getLong();
    } else {
      return null;
    }
  }
}
