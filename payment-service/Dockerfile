
FROM eclipse-temurin:17-jre
ARG PROJECT_VERSION=0.1.0
RUN mkdir -p /home/app
WORKDIR /home/app
ENV SPRING_PROFILES_ACTIVE dev
COPY payment-service/ .
ADD payment-service/target/payment-service-v${PROJECT_VERSION}.jar payment-service.jar
EXPOSE 8400
ENTRYPOINT ["java", "-jar", "payment-service.jar"]


