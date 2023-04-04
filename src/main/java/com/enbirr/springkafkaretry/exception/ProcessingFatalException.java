package com.enbirr.springkafkaretry.exception;

/**
 * When @KafkaListener method throws this exception, failed Kafka record should not be retried.
 */
public class ProcessingFatalException extends ProcessingException {

  public ProcessingFatalException(String message) {
    super(message);
  }
}
