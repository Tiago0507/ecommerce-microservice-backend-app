
FROM eclipse-temurin:17-jre
ARG PROJECT_VERSION=0.1.0
RUN mkdir -p /home/app
WORKDIR /home/app
ENV SPRING_PROFILES_ACTIVE dev
COPY product-service/ .
ADD product-service/target/product-service-v${PROJECT_VERSION}.jar product-service.jar
EXPOSE 8500
ENTRYPOINT ["java", "-jar", "product-service.jar"]


