package com.enbirr.springkafkaretry.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ProcessingService {

  public static final int MESSAGE_MAX_LENGTH = 5;

  public String truncateMessage(String message) {
    if (message.length() > MESSAGE_MAX_LENGTH) {
      return message.substring(0, MESSAGE_MAX_LENGTH) + "...";
    } else {
      return message;
    }
  }
}
