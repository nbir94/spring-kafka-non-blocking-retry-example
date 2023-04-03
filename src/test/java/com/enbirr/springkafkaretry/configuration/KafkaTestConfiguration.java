package com.enbirr.springkafkaretry.configuration;

import com.enbirr.springkafkaretry.consumer.KafkaConsumer;
import com.enbirr.springkafkaretry.consumer.KafkaConsumerAspect;
import com.enbirr.springkafkaretry.service.ProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Launches Kafka Docker container and creates application context necessary for testing.
 * {@link KafkaAutoConfiguration} is used to create all needed Kafka beans – the same happens when
 * we just run SpringBootApplication. It makes tests closer to the real conditions and relieves
 * from the need to manually create the objects necessary for Kafka consumer (e.g. ConsumerFactory,
 * ProducerFactory). Unlike the case of using @SpringBootTest annotation, this configuration creates
 * only the necessary context.
 */
@ImportAutoConfiguration(KafkaAutoConfiguration.class)
@Testcontainers
@EnableAspectJAutoProxy
@Slf4j
public class KafkaTestConfiguration {

  @Container
  public static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(
      DockerImageName.parse("confluentinc/cp-kafka:7.3.2"));

  /**
   * Runs Kafka in Docker container and sets its bootstrap-servers URL into application properties.
   */
  public static class KafkaServerInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(final ConfigurableApplicationContext applicationContext) {
      KAFKA_CONTAINER.start();

      String bootstrapServers = KAFKA_CONTAINER.getBootstrapServers();
      TestPropertyValues.of(
          "spring.kafka.consumer.bootstrap-servers=" + bootstrapServers,
          "spring.kafka.producer.bootstrap-servers=" + bootstrapServers)
          .applyTo(applicationContext.getEnvironment());
      log.info("Kafka broker for tests was launched with bootstrap-servers: {}", bootstrapServers);
    }
  }

  @MockBean
  public ProcessingService processingServiceMock;

  // Bean for the class that is being tested
  @Bean
  public KafkaConsumer kafkaConsumer() {
    return new KafkaConsumer(processingServiceMock);
  }

  // Auxiliary object to catch the moment when Kafka consumer has processed a message.
  @Bean
  public KafkaConsumerAspect kafkaConsumerAspect() {
    return new KafkaConsumerAspect();
  }
}
