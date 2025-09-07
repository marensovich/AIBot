FROM openjdk:17
COPY target/bot-*.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]