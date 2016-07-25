FROM clojure:latest

ADD . /app

WORKDIR /app

ENV KAFKA_SERVER kafka:9092

RUN cd lib/utils && lein uberjar

RUN cd lib/utils && lein localrepo install target/utils-0.2.0-SNAPSHOT-standalone.jar kafka-service 0.0.2

RUN ["lein", "modules", "install"]

CMD ["lein", "run", "dev"]
