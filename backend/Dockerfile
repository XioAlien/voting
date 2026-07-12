FROM mcr.microsoft.com/devcontainers/java:1-17-bookworm AS build
WORKDIR /workspace

RUN rm -f /etc/apt/sources.list.d/yarn.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends maven \
    && rm -rf /var/lib/apt/lists/*

COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests package

FROM mcr.microsoft.com/devcontainers/java:1-17-bookworm
WORKDIR /app

COPY --from=build /workspace/target/backend-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=12 CMD wget -qO- http://127.0.0.1:8080/api/votes >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
