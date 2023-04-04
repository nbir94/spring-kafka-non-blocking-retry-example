package com.enbirr.springkafkaretry.consumer;


import lombok.Getter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Catches the moment when {@link KafkaConsumer} has processed all messages that we expect.
 * The instance of this class can store data from previous tests. To avoid possible mistakes,
 * {@link KafkaConsumerAspect#reset(CountDownLatch)} method should be called in the beginning of
 * each test method where this aspect class is using.
 */
@Aspect
@Getter
public class KafkaConsumerAspect {

  private CountDownLatch successfulExecutionsLatch = new CountDownLatch(0);
  // stores all records consumed from the main and retry topics
  private List<ConsumerRecord<String, String>> consumedRecords = new ArrayList<>();

  // method in @Around value cannot be private: this aspect class should have access to it.
  @Around(value = "execution(* com.enbirr.springkafkaretry.consumer.KafkaConsumer.consumeMessage(..))")
  @SuppressWarnings("unchecked")
  public void afterConsuming(ProceedingJoinPoint pjp) throws Throwable {
    Object[] methodArgs = pjp.getArgs();
    ConsumerRecord<String, String> consumerRecord = (ConsumerRecord<String, String>) methodArgs[0];
    consumedRecords.add(consumerRecord);

    pjp.proceed();
    successfulExecutionsLatch.countDown();
  }

  /**
   * Should be called in the beginning of each test where we want to use this aspect class.
   *
   * @param successfulExecutionsLatch {@link CountDownLatch} with count of messages that we expect
   *                                                        KafkaConsumer to successfully process.
   */
  public void reset(CountDownLatch successfulExecutionsLatch) {
    this.successfulExecutionsLatch = successfulExecutionsLatch;
    this.consumedRecords = new ArrayList<>();
  }
}
