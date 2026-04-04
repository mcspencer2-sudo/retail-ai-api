FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN ./mvnw clean package -DskipTests

CMD ["java", "-jar", "target/retail-ai-api-0.0.1-SNAPSHOT.jar"]