FROM eclipse-temurin:17-jre

ARG PROJECT_VERSION=0.1.0

RUN mkdir -p /home/app
WORKDIR /home/app
ENV SPRING_PROFILES_ACTIVE=dev
COPY target/proxy-client-v${PROJECT_VERSION}.jar proxy-client.jar
EXPOSE 8900
ENTRYPOINT ["java", "-jar", "proxy-client.jar"]