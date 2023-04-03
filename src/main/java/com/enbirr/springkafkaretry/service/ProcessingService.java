package com.enbirr.springkafkaretry.service;

import com.enbirr.springkafkaretry.exception.ProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Slf4j
public class ProcessingService {

  private static final int MESSAGE_MAX_LENGTH = 5;
  private static final Random RANDOM = new Random();

  @Value("${processing.error-percentage:0}")
  private Integer errorPercentage;

  public String truncateMessageInRiskyWay(String message) throws ProcessingException {
    int chanceOfSuccess = RANDOM.nextInt(1, 101);

    if (chanceOfSuccess > errorPercentage) {
      return truncateMessage(message);
    } else {
      throw new ProcessingException();
    }
  }

  private static String truncateMessage(String message) {
    if (message.length() > MESSAGE_MAX_LENGTH) {
      return message.substring(0, MESSAGE_MAX_LENGTH) + "...";
    } else {
      return message;
    }
  }
}
