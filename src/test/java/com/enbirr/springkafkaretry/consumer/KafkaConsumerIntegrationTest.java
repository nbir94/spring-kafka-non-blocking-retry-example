package com.enbirr.springkafkaretry.consumer;

import static com.enbirr.springkafkaretry.constants.AssertionMessages.KafkaConsumer.MESSAGE_SHOULD_BE_RECEIVED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.enbirr.springkafkaretry.configuration.KafkaTestConfiguration;
import com.enbirr.springkafkaretry.service.ProcessingService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.CountDownLatch;

@SpringJUnitConfig(
    classes = KafkaTestConfiguration.class,
    initializers = KafkaTestConfiguration.KafkaServerInitializer.class)
@TestPropertySource(value = "classpath:/application-test.properties")
class KafkaConsumerIntegrationTest {

  private static final long AWAIT_TIMEOUT_SECONDS = 30;
  @Value("${kafka.mainTopic}")
  private String kafkaTopic;

  @Autowired
  private KafkaConsumerAspect kafkaConsumerAspect;
  // Mocked KafkaConsumer's dependency
  @Autowired
  public ProcessingService processingServiceMock;
  @Autowired
  private KafkaTemplate<String, String> kafkaTemplate;

  @Test
  @SneakyThrows
  void givenAnyMessage_whenAppReceivesIt_thenProcessingServiceIsCalled() {
    // GIVEN
    String incomingMessage = "any correct message";
    int expectedMessagesCount = 1;
    // Set up the latch according to the count of messages that consumer should receive.
    CountDownLatch latch = new CountDownLatch(expectedMessagesCount);
    kafkaConsumerAspect.reset(latch);

    kafkaTemplate.send(kafkaTopic, incomingMessage);

    // WHEN
    boolean messageReceived = latch.await(AWAIT_TIMEOUT_SECONDS, SECONDS);

    // THEN
    Assertions.assertTrue(messageReceived, MESSAGE_SHOULD_BE_RECEIVED);
    verify(processingServiceMock, times(expectedMessagesCount)).truncateMessage(incomingMessage);
  }
}
