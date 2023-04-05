package com.enbirr.springkafkaretry.consumer;

import static com.enbirr.springkafkaretry.constants.KafkaTestMessages.DLQ_RECORD_SHOULD_BE_RECEIVED;
import static com.enbirr.springkafkaretry.constants.KafkaTestMessages.MESSAGE_FROM_DLQ_TOPIC_WAS_EXPECTED;
import static com.enbirr.springkafkaretry.constants.KafkaTestMessages.MESSAGE_FROM_RETRY_TOPIC_WAS_EXPECTED;
import static com.enbirr.springkafkaretry.constants.KafkaTestMessages.MESSAGE_SHOULD_BE_RECEIVED;
import static com.enbirr.springkafkaretry.constants.KafkaTestMessages.RECORD_HEADER_SHOULD_BE_SPECIFIED;
import static com.enbirr.springkafkaretry.constants.KafkaTestMessages.RECORD_HEADER_VALUE_MUST_BE_EQUAL_TO;
import static com.enbirr.springkafkaretry.constants.KafkaTestMessages.RECORD_VALUE_SHOULD_CONTAIN_SENT_MESSAGE;
import static com.enbirr.springkafkaretry.constants.KafkaTestMessages.RETRY_RECORD_SHOULD_BE_RECEIVED;
import static com.enbirr.springkafkaretry.constants.LogAndExceptionMessages.PROCESSING_ERROR_COULD_BE_RETRIED;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.kafka.support.KafkaHeaders.EXCEPTION_CAUSE_FQCN;
import static org.springframework.kafka.support.KafkaHeaders.EXCEPTION_FQCN;
import static org.springframework.kafka.support.KafkaHeaders.EXCEPTION_MESSAGE;

import com.enbirr.springkafkaretry.configuration.KafkaCustomProperties;
import com.enbirr.springkafkaretry.configuration.KafkaRetryDlqConfiguration;
import com.enbirr.springkafkaretry.configuration.KafkaTestConfiguration;
import com.enbirr.springkafkaretry.exception.ProcessingException;
import com.enbirr.springkafkaretry.exception.ProcessingFatalException;
import com.enbirr.springkafkaretry.service.ProcessingService;
import com.enbirr.springkafkaretry.util.TestDataGenerator;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

@SpringJUnitConfig(
    classes = {
        KafkaTestConfiguration.class,
        KafkaRetryDlqConfiguration.class,
        KafkaCustomProperties.class},
    initializers = KafkaTestConfiguration.KafkaServerInitializer.class)
@TestPropertySource(value = "classpath:/application-test.properties")
class KafkaConsumerIntegrationTest {

  private static final long AWAIT_TIMEOUT = 30;
  @Autowired
  private KafkaCustomProperties kafkaProperties;
  @Autowired
  private KafkaTemplate<String, String> kafkaTemplate;
  @Autowired
  private BlockingQueue<ConsumerRecord<String, String>> retryAndDlqRecords;
  @Autowired
  private KafkaConsumerAspect kafkaConsumerAspect;
  // Mocked KafkaConsumer's dependency
  @Autowired
  public ProcessingService processingServiceMock;

  @Test
  @SneakyThrows
  void givenAnyMessage_whenAppReceivesIt_thenProcessingServiceIsCalled() {
    // GIVEN
    String incomingMessage = "any correct message";
    int expectedMessagesCount = 1;
    // Set up the latch according to the count of messages that consumer should receive.
    CountDownLatch consumerLatch = new CountDownLatch(expectedMessagesCount);
    kafkaConsumerAspect.reset(consumerLatch);

    kafkaTemplate.send(kafkaProperties.getMainTopic(), incomingMessage);

    // WHEN
    boolean messageReceived = consumerLatch.await(AWAIT_TIMEOUT, SECONDS);

    // THEN
    Assertions.assertTrue(messageReceived, MESSAGE_SHOULD_BE_RECEIVED);
    verify(processingServiceMock, times(expectedMessagesCount))
        .truncateMessageInRiskyWay(incomingMessage);
  }

  @Test
  @SneakyThrows
  void givenProcessingAlwaysFails_whenAppGetsRecord_thenItComesToRetryTopicAndThenToDlq() {
    // GIVEN
    String incomingMessage = "any unprocessable message";

    ProcessingException exception = new ProcessingException(PROCESSING_ERROR_COULD_BE_RETRIED);
    doThrow(exception).when(processingServiceMock).truncateMessageInRiskyWay(incomingMessage);

    // WHEN-THEN
    kafkaTemplate.send(kafkaProperties.getMainTopic(), incomingMessage);

    waitAndVerifyRetryRecords(incomingMessage, exception);
    waitAndVerifyDlqRecord(incomingMessage, exception);

    verify(processingServiceMock, times(kafkaProperties.getAttemptsMaxCount()))
        .truncateMessageInRiskyWay(incomingMessage);
  }

  @Test
  @SneakyThrows
  void givenFatalException_whenItIsThrown_thenMessageComesToDlqWithoutRetry() {
    // GIVEN
    String incomingMessage = "any unprocessable message";
    ProcessingFatalException fatalException = TestDataGenerator.createProcessingFatalException();
    doThrow(fatalException).when(processingServiceMock).truncateMessageInRiskyWay(incomingMessage);

    // WHEN-THEN
    kafkaTemplate.send(kafkaProperties.getMainTopic(), incomingMessage);

    waitAndVerifyDlqRecord(incomingMessage, fatalException);

    verify(processingServiceMock, times(1))
        .truncateMessageInRiskyWay(incomingMessage);
  }

  @SneakyThrows
  private void waitAndVerifyRetryRecords(String expectedPayload, Exception occurredException) {
    String retryTopic = kafkaProperties.getMainTopic() + kafkaProperties.getRetryTopicSuffix();
    int attemptsForMainTopic = 1;
    int expectedRetriesCount = kafkaProperties.getAttemptsMaxCount() - attemptsForMainTopic;

    for (int i = 0; i < expectedRetriesCount; i++) {
      ConsumerRecord<String, String> retryRecord = retryAndDlqRecords.poll(AWAIT_TIMEOUT, SECONDS);
      assertNotNull(retryRecord, RETRY_RECORD_SHOULD_BE_RECEIVED);
      assertEquals(retryTopic, retryRecord.topic(), MESSAGE_FROM_RETRY_TOPIC_WAS_EXPECTED);
      assertEquals(expectedPayload, retryRecord.value(), RECORD_VALUE_SHOULD_CONTAIN_SENT_MESSAGE);
      assertExceptionHeadersAreValid(retryRecord.headers(), occurredException);
    }
  }

  @SneakyThrows
  private void waitAndVerifyDlqRecord(String expectedPayload, Exception occurredException) {
    String dlqTopic = kafkaProperties.getMainTopic() + kafkaProperties.getDlqTopicSuffix();
    ConsumerRecord<String, String> dlqRecord = retryAndDlqRecords.poll(AWAIT_TIMEOUT, SECONDS);
    assertNotNull(dlqRecord, DLQ_RECORD_SHOULD_BE_RECEIVED);
    assertEquals(dlqTopic, dlqRecord.topic(), MESSAGE_FROM_DLQ_TOPIC_WAS_EXPECTED);
    assertEquals(expectedPayload, dlqRecord.value(), RECORD_VALUE_SHOULD_CONTAIN_SENT_MESSAGE);

    assertExceptionHeadersAreValid(dlqRecord.headers(), occurredException);
  }

  private void assertExceptionHeadersAreValid(Headers verifiableHeaders, Exception occurredException) {
    assertHeaderIsValid(EXCEPTION_MESSAGE, occurredException.getMessage(), verifiableHeaders);
    assertHeaderIsValid(EXCEPTION_FQCN, occurredException.getClass().getName(), verifiableHeaders);
    if (occurredException.getCause() != null) {
      String causeClassName = occurredException.getCause().getClass().getName();
      assertHeaderIsValid(EXCEPTION_CAUSE_FQCN, causeClassName, verifiableHeaders);
    } else {
      Header exceptionCauseHeader = verifiableHeaders.lastHeader(EXCEPTION_CAUSE_FQCN);
      assertNull(exceptionCauseHeader);
    }
  }

  private void assertHeaderIsValid(
      String expectedHeaderKey,
      String expectedHeaderValue,
      Headers verifiableHeaders
  ) {
    Header header = verifiableHeaders.lastHeader(expectedHeaderKey);
    String msg = String.format(RECORD_HEADER_SHOULD_BE_SPECIFIED, expectedHeaderKey);
    assertNotNull(header, msg);

    msg = String.format(RECORD_HEADER_VALUE_MUST_BE_EQUAL_TO, expectedHeaderKey, expectedHeaderValue);
    assertEquals(expectedHeaderValue, new String(header.value(), UTF_8), msg);
  }
}
