# Spring Kafka non-blocking-retry (example)

An example of a Spring Boot Kafka consumer application with non-blocking retry.
## Table of Contents
- [The main logic](#the-main-logic)
- [Manual testing](#manual-testing)
- [Retry logic details](#retry-logic-details)
- [Integration tests](#integration-tests)

## The main logic
1. @KafkaListener annotated method receives a String message from the ***main*** Kafka topic.
2. The Application tries to process this String message (trim payload if its length > 5 symbols) using a method that could throw an exception (you can set up error possibility in [application.properties](src/main/resources/application.properties)). Possible cases: 
    - **app prints the result in logs** if processing succeeds;
    - **app sends failed record to retry topic** if @KafkaListener annotated method throws an exception. After delayed time, this method will attempt to process failed record again from a retry topic (see the first step);
    - **app sends failed record to DLQ topic** (dead letter queue) if it has exhausted all attempts to process this record.
    - **app sends failed record to DLQ topic without retries** if specific fatal exception was thrown.

## Manual testing
1) Create Kafka environment using [docker-compose.yml](docker-compose.yml):
  ```shell
  docker-compose up -d
  ```
2) (Optional) Set up [application.properties](src/main/resources/application.properties) if needed.
    <details>
      <summary>Some useful properties</summary>
   
    - **kafka.retry.attempts-count** — count of attempts to process the message. First attempt is for reading from the main topic, the next ones — from the retry topic.
    - **kafka.retry.interval-ms** — interval between attempts in milliseconds.
    - **processing.error-percentage** — probability in percent of throwing processing exception. When this exception is thrown, application could send failed record to the retry topic.
    - **processing.fatal-error-percentage** — probability in percent of throwing **fatal** processing exception. When this exception is thrown, application immediately sends failed record to the DLQ topic.
    - **processing.message-max-length** — if an incoming message has longer length, the application will trim it up to this value.
    </details>

3) Run [SpringKafkaApplication.java](src/main/java/com/enbirr/springkafkaretry/SpringKafkaApplication.java) by using your IDE or by executing next commands:
- Build .jar file:
```shell
mvn clean package 
```
- Run Java app from this .jar:
```shell
java -jar "target/spring-kafka-non-blocking-retry.jar"
```
4) Run Kafka producer in command line to send passages into the main kafka topic:
```shell
sh manualTesting/produce-message.sh
```
5) Look into the SpringKafkaApplication logs and check if the app has processed the message.

## Retry logic details
- **Retry mechanism in this example doesn't block receiving of messages from the main topic.** If we didn't configure the retry logic in any way, by default Spring would make 9 retries to process the message using the [DefaultErrorHandler](https://docs.spring.io/spring-kafka/reference/html/#default-eh). It seeks consumer to the current offset again and again, so it blocks receiving messages from the main topic.
- **In this example, we use only one retry topic.** By default, if we want application to make **N** retries (N > 0) using a retry topic, Spring creates N topics: 1 topic per a retry. This is not bad, but on some projects the regulations are aimed at a minimum topics count.
- **This example shows how to set up your own retry and DLQ topic names.** By default, Spring uses "-retry" and "-dlt" suffixes for the retry and DLQ topics. It can break naming rules for some projects, which don't use dash symbol in names.
- This example demonstrates **how to set up informative Kafka record headers for a retry and DLQ topics**. By default, Springs wraps any exception going from @KafkaListener method into the ListenerExecutionFailedException. It is used in the internal Spring logic. But information from this exception in the Kafka headers **doesn't clarify the occurred problem at all.**
## Integration tests
- Integration tests for Kafka logic use almost the same configuration as a real-time running application would have. 
- These tests are running rather quickly. Because they create only the context that is necessary for testing Kafka functionality.
- Dependencies used in tests:
  - **JUnit5** 
  - **Mockito**
  - **Testcontainers** (to launch Kafka broker in Docker container)
  - **Spring Aspects** (to catch a moment when Kafka consumer has processed the message)

You can run these tests by executing the next command:
```shell
mvn clean test
```
