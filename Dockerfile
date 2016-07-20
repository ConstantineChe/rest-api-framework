FROM clojure:latest

ADD . /app

WORKDIR /app

ENV KAFKA_SERVER kafka:9092

RUN ["lein", "modules", "install"]

RUN lein uberjar

CMD ["java", "-jar", "target/uberjar-services/services-0.0.1-SNAPSHOT-standalone.jar"]
