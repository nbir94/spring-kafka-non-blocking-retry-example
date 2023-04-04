package com.enbirr.springkafkaretry.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LogAndExceptionMessages {

  public static final String SETTING_UP_RETRY_TOPIC =
      "Setting up retry topic: for each message from the main topic there will be {} attempts "
          + "made with an interval of {} milliseconds between them. After each attempt, the "
          + "message gets forwarded either to the retry topic or to the dlq.";
}
