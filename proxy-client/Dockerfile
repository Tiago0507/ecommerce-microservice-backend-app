
FROM eclipse-temurin:17-jre
ARG PROJECT_VERSION=0.1.0
RUN mkdir -p /home/app
WORKDIR /home/app
ENV SPRING_PROFILES_ACTIVE dev
COPY proxy-client/ .
ADD proxy-client/target/proxy-client-v${PROJECT_VERSION}.jar proxy-client.jar
EXPOSE 8900
# Rely on SPRING_PROFILES_ACTIVE env var (Spring Boot reads it nativamente)
ENTRYPOINT ["java", "-jar", "proxy-client.jar"]


