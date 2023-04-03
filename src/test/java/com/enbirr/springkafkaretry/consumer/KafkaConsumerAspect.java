package com.enbirr.springkafkaretry.consumer;


import lombok.Getter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.concurrent.CountDownLatch;

/**
 * Allows to catch the moment when {@link KafkaConsumer} has processed all messages that we expect.
 */
@Aspect
@Getter
public class KafkaConsumerAspect {

  private CountDownLatch successfulExecutionsLatch;

  // consumeMessage(...) method in Kafka consumer cannot be private: it should be accessible from
  // this aspect class.
  @Around(value = "execution(* com.enbirr.springkafkaretry.consumer.KafkaConsumer.consumeMessage(..))")
  public void afterConsuming(ProceedingJoinPoint pjp) throws Throwable {
    pjp.proceed();
    successfulExecutionsLatch.countDown();
  }

  /**
   * This method should be called in each test where we want to use this aspect class (before data
   * sending to the consumer).
   *
   * @param successfulExecutionsLatch {@link CountDownLatch} with count of messages that we expect
   *                                                        KafkaConsumer to successfully process.
   */
  public void reset(CountDownLatch successfulExecutionsLatch) {
    this.successfulExecutionsLatch = successfulExecutionsLatch;
  }
}
