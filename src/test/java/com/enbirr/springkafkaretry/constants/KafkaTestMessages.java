package com.enbirr.springkafkaretry.constants;

public class KafkaTestMessages {

  public static final String MESSAGE_SHOULD_BE_RECEIVED = "Message should be received by Kafka "
      + "consumer";
  public static final String RETRY_RECORD_SHOULD_BE_RECEIVED = "Retry record should be "
      + "received by auxiliary Retry / DLQ listener";
  public static final String DLQ_RECORD_SHOULD_BE_RECEIVED = "DLQ record should be "
      + "received by auxiliary Retry / DLQ listener";
  public static final String MESSAGE_FROM_DLQ_TOPIC_WAS_EXPECTED = "It's expected to receive a "
      + "message from DLQ topic";
  public static final String MESSAGE_FROM_RETRY_TOPIC_WAS_EXPECTED = "It's expected to receive a "
      + "message from retry topic";
  public static final String RECORD_VALUE_SHOULD_CONTAIN_SENT_MESSAGE = "Kafka record value for"
      + "all topics (main, retry, DLQ) should be equal to the message that was sent";
  public static final String RETRY_DLQ_TEST_LISTENER_RECEIVED_A_RECORD = "Retry & DLQ Test "
      + "Listener received a record (topic = {}, partition = {}, offset = {}): '{}'";
}