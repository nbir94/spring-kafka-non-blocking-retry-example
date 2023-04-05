package com.enbirr.springkafkaretry.util;

import static com.enbirr.springkafkaretry.constants.LogAndExceptionMessages.FATAL_PROCESSING_ERROR_NOT_RETRIABLE;
import static com.enbirr.springkafkaretry.constants.LogAndExceptionMessages.PROCESSING_ERROR_CAUSE;

import com.enbirr.springkafkaretry.exception.ProcessingException;
import com.enbirr.springkafkaretry.exception.ProcessingFatalException;
import org.jetbrains.annotations.NotNull;

public class TestDataGenerator {

  @NotNull
  public static ProcessingFatalException createProcessingFatalException() {
    ProcessingException cause = new ProcessingException(PROCESSING_ERROR_CAUSE);
    return new ProcessingFatalException(FATAL_PROCESSING_ERROR_NOT_RETRIABLE, cause);
  }
}
