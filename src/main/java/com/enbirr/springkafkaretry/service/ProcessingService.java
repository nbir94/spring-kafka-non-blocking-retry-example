package com.enbirr.springkafkaretry.service;

import static com.enbirr.springkafkaretry.constants.LogAndExceptionMessages.FATAL_PROCESSING_ERROR_NOT_RETRIABLE;
import static com.enbirr.springkafkaretry.constants.LogAndExceptionMessages.PROCESSING_ERROR_CAUSE;
import static com.enbirr.springkafkaretry.constants.LogAndExceptionMessages.PROCESSING_ERROR_COULD_BE_RETRIED;

import com.enbirr.springkafkaretry.exception.ProcessingException;
import com.enbirr.springkafkaretry.exception.ProcessingFatalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Slf4j
public class ProcessingService {

  private static final Random RANDOM = new Random();

  @Value("${processing.message-max-length:5}")
  private Integer messageMaxLength;
  @Value("${processing.error-percentage:0}")
  private Integer errorPercentage;
  @Value("${processing.fatal-error-percentage:0}")
  private Integer fatalErrorPercentage;

  public String truncateMessageInRiskyWay(String message) throws ProcessingException {
    int chanceOfSuccess = RANDOM.nextInt(1, 101);
    maybeRunIntoTheFatalException(chanceOfSuccess);
    maybeRunIntoRetryableException(chanceOfSuccess);
    return truncateMessage(message, messageMaxLength);
  }

  private void maybeRunIntoRetryableException(int chanceOfSuccess) {
    if (chanceOfSuccess <= errorPercentage) {
      throw new ProcessingException(PROCESSING_ERROR_COULD_BE_RETRIED);
    }
  }

  private void maybeRunIntoTheFatalException(int chanceOfSuccess) {
    if (chanceOfSuccess <= fatalErrorPercentage) {
      try {
        throw new ProcessingException(PROCESSING_ERROR_CAUSE);
      } catch (ProcessingException cause) {
        throw new ProcessingFatalException(FATAL_PROCESSING_ERROR_NOT_RETRIABLE, cause);
      }
    }
  }

  private static String truncateMessage(String message, int messageMaxLength) {
    if (message.length() > messageMaxLength) {
      return message.substring(0, messageMaxLength) + "...";
    } else {
      return message;
    }
  }
}
