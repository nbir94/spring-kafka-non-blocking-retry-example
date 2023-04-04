package com.enbirr.springkafkaretry.exception;

public class ProcessingException extends RuntimeException {

  public ProcessingException(String message) {
    super(message);
  }
}
