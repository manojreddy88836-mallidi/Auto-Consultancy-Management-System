# Backend Dockerfile
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/uploads
VOLUME ["/app/uploads"]
EXPOSE 8080

# JVM tuning for Render containers:
# - UseContainerSupport: respect container memory limits
# - MaxRAMPercentage: use up to 75% of container RAM
# - G1GC: low-pause garbage collector
# - TieredCompilation + tiered-stop-at-level=1: faster startup (skip full JIT on cold start)
# - spring.jmx.enabled=false: skip JMX startup overhead
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-XX:G1HeapRegionSize=4m", \
  "-XX:MaxGCPauseMillis=200", \
  "-XX:+TieredCompilation", \
  "-Dspring.jmx.enabled=false", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
