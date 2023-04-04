printf "Please, type any message. Press Enter to send it to Kafka. \n"

docker exec -it retry-example-broker kafka-console-producer \
  --bootstrap-server retry-example-broker:9092 \
  --topic spring.kafka.example \
