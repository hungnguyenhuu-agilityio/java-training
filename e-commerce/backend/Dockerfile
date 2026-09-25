FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /application
RUN addgroup -S ecommerce && adduser -S ecommerce -G ecommerce
COPY --from=build /workspace/target/ecommerce-*.jar application.jar
USER ecommerce
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/application/application.jar"]
