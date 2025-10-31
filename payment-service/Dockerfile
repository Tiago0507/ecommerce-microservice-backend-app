FROM eclipse-temurin:17-jre

ARG PROJECT_VERSION=0.1.0

RUN mkdir -p /home/app
WORKDIR /home/app
ENV SPRING_PROFILES_ACTIVE=dev
COPY target/payment-service-v${PROJECT_VERSION}.jar payment-service.jar
EXPOSE 8300
ENTRYPOINT ["java", "-jar", "payment-service.jar"]