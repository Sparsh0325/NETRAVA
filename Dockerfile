FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/target/intellishare-backend-*.jar /app/app.jar
EXPOSE 8085
USER 10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
