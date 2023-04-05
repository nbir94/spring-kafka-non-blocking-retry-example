package com.enbirr.springkafkaretry.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.kafka.retrytopic.RetryTopicHeaders.DEFAULT_HEADER_ATTEMPTS;
import static org.springframework.kafka.support.KafkaHeaders.ORIGINAL_OFFSET;
import static org.springframework.kafka.support.KafkaHeaders.ORIGINAL_PARTITION;

import com.enbirr.springkafkaretry.exception.ProcessingException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer.HeaderNames;
import org.springframework.kafka.support.KafkaHeaders;

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

  /**
   * Sets up custom exception headers in Kafka records for the retry and DLQ topics. By default,
   * Springs wraps any exception going from @KafkaListener method into the
   * {@link org.springframework.kafka.listener.ListenerExecutionFailedException}. It is used in the
   * internal Spring logic. But information from this exception in the Kafka headers does not
   * clarify the occurred problem at all.
   *
   * @param kafkaHeaders      the {@link Headers} to add the header(s) to.
   * @param occurredException the exception.
   * @param isKey             whether the exception is for a key or value.
   * @param headerNames       the heaader names to use.
   * @see org.springframework.kafka.listener.DeadLetterPublishingRecoverer.ExceptionHeadersCreator
   * The set of params is defined by ExceptionHeadersCreator.
   */
  public static void getCustomExceptionHeadersCreator(
      Headers kafkaHeaders,
      Exception occurredException,
      boolean isKey,
      HeaderNames headerNames
  ) {
    Throwable exception = findInformativeExceptionInCauseChain(occurredException);
    // avoid duplicating of old headers
    removeOldExceptionHeaders(kafkaHeaders);

    kafkaHeaders.add(KafkaHeaders.EXCEPTION_FQCN, exception.getClass().getName().getBytes(UTF_8));

    if (ObjectUtils.isNotEmpty(exception.getMessage())) {
      kafkaHeaders.add(KafkaHeaders.EXCEPTION_MESSAGE, exception.getMessage().getBytes(UTF_8));
    }

    if (exception.getCause() != null) {
      String causeClassName = exception.getCause().getClass().getName();
      kafkaHeaders.add(KafkaHeaders.EXCEPTION_CAUSE_FQCN, causeClassName.getBytes(UTF_8));
    }
  }

  private static Throwable findInformativeExceptionInCauseChain(Exception exception) {
    int exIndex = ExceptionUtils.indexOfType(exception, ProcessingException.class);
    if (exIndex != -1) {
      return ExceptionUtils.getThrowables(exception)[exIndex];
    } else {
      return exception;
    }
  }

  private static void removeOldExceptionHeaders(Headers kafkaHeaders) {
    kafkaHeaders.remove(KafkaHeaders.EXCEPTION_FQCN);
    kafkaHeaders.remove(KafkaHeaders.EXCEPTION_MESSAGE);
    kafkaHeaders.remove(KafkaHeaders.EXCEPTION_CAUSE_FQCN);
  }
}
