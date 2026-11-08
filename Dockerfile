FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY . .
RUN ./gradlew bootJar --no-daemon
EXPOSE 8080
CMD ["java", "-jar", "build/libs/otus-java-professional-urlshortener-0.0.1-SNAPSHOT.jar"]
