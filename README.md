# spring-kafka-non-blocking-retry-example

An example of a Spring Boot Kafka consumer application with non-blocking retry. 

## The main logic
1. @KafkaListener annotated method receives a String message from the ***main*** Kafka topic.
2. The Application tries to process this String message (trim payload if its length > 5 symbols) using a method that could throw an exception (you can set up error possibility in [application.properties](src/main/resources/application.properties)). Possible cases: 
    - **app prints the result in logs** if processing succeeds;
    - **app sends failed record to retry topic** if @KafkaListener annotated method throws an exception. After delayed time, this method will attempt to process failed record again from a retry topic (see the first step);
    - **app sends failed record to DLQ topic** (dead letter queue) if it has exhausted all attempts to process this record.

## Retry logic details
- **Retry mechanism in this example doesn't block receiving of messages from the main topic.** If we didn't configure the retry logic in any way, by default Spring would make 9 retries to process the message using the DefaultErrorHandler. It seeks consumer to the current offset again and again, so it blocks receiving messages from the main topic.
- **In this example, we use only one retry topic.** By default, if we want application to make **N** retries (N > 0) using a retry topic, Spring creates N topics: 1 topic per a retry. This is not bad, but on some projects the regulations are aimed at a minimum topics count.
- **This example shows how to set up your own retry and DLQ topic names.** By default, Spring uses "-retry" and "-dlt" suffixes for the retry and DLQ topics. It can break naming rules for some projects, which don't use dash symbol in names. 
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
## Manual testing
1) Create Kafka environment using [docker-compose.yml](docker-compose.yml):
  ```shell
  docker-compose up -d
  ```
2) Run [SpringKafkaApplication.java](src/main/java/com/enbirr/springkafkaretry/SpringKafkaApplication.java) by using your IDE or by executing next commands:
- Build .jar file:
```shell
mvn clean package 
```
- Run Java app from this .jar:
```shell
java -jar "target/spring-kafka-non-blocking-retry.jar"
```
3) Run Kafka producer in command line to send passages into the main kafka topic:
```shell
sh manualTesting/produce-message.sh
```
4) Look into the SpringKafkaApplication logs and check if the app has processed the message. 
