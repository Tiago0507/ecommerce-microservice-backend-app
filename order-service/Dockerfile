FROM eclipse-temurin:17-jre

ARG PROJECT_VERSION=0.1.0

RUN mkdir -p /home/app
WORKDIR /home/app
ENV SPRING_PROFILES_ACTIVE=dev
COPY target/order-service-v${PROJECT_VERSION}.jar order-service.jar
EXPOSE 8300
ENTRYPOINT ["java", "-jar", "order-service.jar"]