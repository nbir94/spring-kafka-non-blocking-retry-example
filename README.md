# spring-kafka-non-blocking-retry-example

An example of a Spring Boot Kafka consumer application ~~with non-blocking retry~~ (not yet implemented). 

## The main logic
1. @KafkaListener annotated method receives a String message from the ***main*** Kafka topic.
2. Application tries to process this String message (trim payload if its length > 5 symbols) using a method that could throw an exception (you can set up error possibility in [application.properties](src/main/resources/application.properties)). Possible cases: 
   - **app prints the result in logs** if processing succeeds;
   - **otherwise, the default Spring logic triggers**. If @KafkaListener annotated method throws any exception, DefaultErrorHandler seeks consumer to the current offset to reprocess the failed record. There are 9 retries at all and each one blocks receiving of other messages from the main topic.
## Integration tests
- Integration tests for Kafka logic use almost the same configuration as a real-time running application would have. 
- These tests are executing rather quickly. Because they create only the context that is necessary for testing Kafka functionality.
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
4) Look into the SpringKafkaApplication logs and check if app has processed the message. 
