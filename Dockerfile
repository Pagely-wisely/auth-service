FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
COPY . .
ARG GPR_USER
ARG GPR_KEY
ENV GPR_USER=${GPR_USER}
ENV GPR_KEY=${GPR_KEY}
RUN chmod +x gradlew
RUN ./gradlew bootJar --no-daemon -Pgpr.user="${GPR_USER}" -Pgpr.key="${GPR_KEY}"
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar
EXPOSE 19091
ENTRYPOINT ["java","-jar","/app/app.jar"]
