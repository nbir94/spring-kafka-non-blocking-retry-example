package com.enbirr.springkafkaretry.consumer;

import static com.enbirr.springkafkaretry.constants.KafkaTestMessages.DLQ_RECORD_SHOULD_BE_RECEIVED;
import static com.enbirr.springkafkaretry.constants.KafkaTestMessages.MESSAGE_FROM_DLQ_TOPIC_WAS_EXPECTED;
import static com.enbirr.springkafkaretry.constants.KafkaTestMessages.MESSAGE_FROM_RETRY_TOPIC_WAS_EXPECTED;
import static com.enbirr.springkafkaretry.constants.KafkaTestMessages.MESSAGE_SHOULD_BE_RECEIVED;
import static com.enbirr.springkafkaretry.constants.KafkaTestMessages.RECORD_VALUE_SHOULD_CONTAIN_SENT_MESSAGE;
import static com.enbirr.springkafkaretry.constants.KafkaTestMessages.RETRY_RECORD_SHOULD_BE_RECEIVED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.enbirr.springkafkaretry.configuration.KafkaCustomProperties;
import com.enbirr.springkafkaretry.configuration.KafkaRetryDlqConfiguration;
import com.enbirr.springkafkaretry.configuration.KafkaTestConfiguration;
import com.enbirr.springkafkaretry.exception.ProcessingException;
import com.enbirr.springkafkaretry.exception.ProcessingFatalException;
import com.enbirr.springkafkaretry.service.ProcessingService;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
    when(processingServiceMock.truncateMessageInRiskyWay(incomingMessage))
        .thenThrow(ProcessingException.class);

    // WHEN-THEN
    kafkaTemplate.send(kafkaProperties.getMainTopic(), incomingMessage);

    waitAndVerifyRetryRecords(incomingMessage);
    waitAndVerifyDlqRecord(incomingMessage);

    verify(processingServiceMock, times(kafkaProperties.getAttemptsMaxCount()))
        .truncateMessageInRiskyWay(incomingMessage);
  }

  @Test
  @SneakyThrows
  void givenFatalException_whenItIsThrown_thenMessageComesToDlqWithoutRetry() {
    // GIVEN
    String incomingMessage = "any unprocessable message";
    when(processingServiceMock.truncateMessageInRiskyWay(incomingMessage))
        .thenThrow(ProcessingFatalException.class);

    // WHEN-THEN
    kafkaTemplate.send(kafkaProperties.getMainTopic(), incomingMessage);

    waitAndVerifyDlqRecord(incomingMessage);

    verify(processingServiceMock, times(1))
        .truncateMessageInRiskyWay(incomingMessage);
  }

  @SneakyThrows
  private void waitAndVerifyRetryRecords(String expectedPayload) {
    String retryTopic = kafkaProperties.getMainTopic() + kafkaProperties.getRetryTopicSuffix();
    int attemptsForMainTopic = 1;
    int expectedRetriesCount = kafkaProperties.getAttemptsMaxCount() - attemptsForMainTopic;

    for (int i = 0; i < expectedRetriesCount; i++) {
      ConsumerRecord<String, String> retryRecord = retryAndDlqRecords.poll(AWAIT_TIMEOUT, SECONDS);
      assertNotNull(retryRecord, RETRY_RECORD_SHOULD_BE_RECEIVED);
      assertEquals(retryTopic, retryRecord.topic(), MESSAGE_FROM_RETRY_TOPIC_WAS_EXPECTED);
      assertEquals(expectedPayload, retryRecord.value(), RECORD_VALUE_SHOULD_CONTAIN_SENT_MESSAGE);
    }
  }

  @SneakyThrows
  private void waitAndVerifyDlqRecord(String expectedPayload) {
    String dlqTopic = kafkaProperties.getMainTopic() + kafkaProperties.getDlqTopicSuffix();
    ConsumerRecord<String, String> dlqRecord = retryAndDlqRecords.poll(AWAIT_TIMEOUT, SECONDS);
    assertNotNull(dlqRecord, DLQ_RECORD_SHOULD_BE_RECEIVED);
    assertEquals(dlqTopic, dlqRecord.topic(), MESSAGE_FROM_DLQ_TOPIC_WAS_EXPECTED);
    assertEquals(expectedPayload, dlqRecord.value(), RECORD_VALUE_SHOULD_CONTAIN_SENT_MESSAGE);
  }
}
