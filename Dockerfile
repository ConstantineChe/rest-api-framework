FROM clojure:latest

ADD . /app

WORKDIR /app

ENV KAFKA_SERVER kafka:9092

RUN ["lein", "modules", "install"]

EXPOSE 8080

CMD ["lein", "run", "dev"]
