FROM maven:3.10.2-eclipse-temurin-25

WORKDIR /app

COPY . .

RUN mvn -B -DskipTests clean compile

EXPOSE 10000

CMD ["mvn", "-B", "exec:java"]