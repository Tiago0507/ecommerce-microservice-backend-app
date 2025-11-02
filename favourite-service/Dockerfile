FROM eclipse-temurin:17-jre

ARG PROJECT_VERSION=0.1.0

RUN mkdir -p /home/app
WORKDIR /home/app
ENV SPRING_PROFILES_ACTIVE=dev
COPY target/favourite-service-v${PROJECT_VERSION}.jar favourite-service.jar
EXPOSE 8800
ENTRYPOINT ["java", "-jar", "favourite-service.jar"]