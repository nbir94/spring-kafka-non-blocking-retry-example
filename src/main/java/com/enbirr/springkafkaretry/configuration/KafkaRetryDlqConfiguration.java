package com.enbirr.springkafkaretry.configuration;


import static com.enbirr.springkafkaretry.constants.LogAndExceptionMessages.SETTING_UP_RETRY_TOPIC;

import com.enbirr.springkafkaretry.exception.ProcessingFatalException;
import com.enbirr.springkafkaretry.util.KafkaUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.retrytopic.DeadLetterPublishingRecovererFactory;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationSupport;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.function.Consumer;

/**
 * By overriding methods of superclass, you can customize the Retry and DLQ logic.
 */
@Configuration
@EnableKafka
@RequiredArgsConstructor
@Slf4j
public class KafkaRetryDlqConfiguration extends RetryTopicConfigurationSupport {

  private final KafkaCustomProperties kafkaProperties;

  @Override
  protected Consumer<DeadLetterPublishingRecovererFactory> configureDeadLetterPublishingContainerFactory() {
    return (DeadLetterPublishingRecovererFactory recovererFactory) -> {
      // Remove old retry headers before resending a Kafka record to the retry topic.
      recovererFactory.setRetainAllRetryHeaderValues(false);
      // Set up more informative exception headers
      recovererFactory.setDeadLetterPublishingRecovererCustomizer(
          (DeadLetterPublishingRecoverer recoverer) ->
              recoverer.setExceptionHeadersCreator(KafkaUtils::getCustomExceptionHeadersCreator));
    };
  }

  @Bean
  public RetryTopicConfiguration setUpRetryTopic(KafkaTemplate<String, String> template) {
    int attemptsMaxCount = kafkaProperties.getAttemptsMaxCount();
    int attemptsIntervalMs = kafkaProperties.getAttemptsIntervalMs();
    log.info(SETTING_UP_RETRY_TOPIC, attemptsMaxCount, attemptsIntervalMs);

    return RetryTopicConfigurationBuilder
        .newInstance()
        .maxAttempts(attemptsMaxCount)
        .fixedBackOff(attemptsIntervalMs)
        .notRetryOn(ProcessingFatalException.class)
        // sets up single topic usage for all retries
        .useSingleTopicForSameIntervals()
        .retryTopicSuffix(kafkaProperties.getRetryTopicSuffix())
        .dltSuffix(kafkaProperties.getDlqTopicSuffix())
        .create(template);
  }

  // KafkaConsumerBackoffManager requires this bean
  @Bean
  public TaskScheduler scheduler() {
    return new ThreadPoolTaskScheduler();
  }
}
