package com.enbirr.springkafkaretry.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LogAndExceptionMessages {

  public static final String SETTING_UP_RETRY_TOPIC =
      "Setting up retry topic: for each message from the main topic there will be {} attempts "
          + "made with an interval of {} milliseconds between them. After each attempt, the "
          + "message gets forwarded either to the retry topic or to the dlq.";
  public static final String MESSAGE_RECEIVED = "Message received ({}). Payload: \"{}\"";
  public static final String MESSAGE_PROCESSED = "Message successfully processed ({}). Result: "
      + "\"{}\"";
  public static final String MESSAGE_FAILED_TO_PROCESS = "Message was failed to process ({})";
  public static final String FATAL_PROCESSING_ERROR_NOT_RETRIABLE = "FATAL PROCESSING ERROR! "
      + "Message should be immediately sent to the DLQ without any retries.";
  public static final String PROCESSING_ERROR_COULD_BE_RETRIED = "Failed to process the message. "
      + "If it's not last attempt, message should be sent to the retry topic and processed after "
      + "delayed time.";
  public static final String PROCESSING_ERROR_CAUSE = "Failed to process the message due to the "
      + "\"Foo Bar\" reason.";
}
