FROM clojure:latest

ADD . /app

WORKDIR /app

ENV KAFKA_SERVER kafka:9092

RUN lein deps

RUN cd lib/kafka-service && lein uberjar

RUN cd lib/kafka-service && lein localrepo install target/kafka-service-0.1.0-SNAPSHOT-standalone.jar kafka-service 0.0.2

RUN ["lein", "modules", "install"]

CMD ["lein", "run", "dev"]
