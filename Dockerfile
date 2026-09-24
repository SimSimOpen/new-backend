FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY . .
# Create gradle.properties inside the build container using secrets
RUN --mount=type=secret,id=gradle_user \
    --mount=type=secret,id=gradle_token \
    mkdir -p /root/.gradle && \
    printf "gpr.user=%s\ngpr.key=%s\n" \
      "$(cat /run/secrets/gradle_user)" \
      "$(cat /run/secrets/gradle_token)" \
      > /root/.gradle/gradle.properties && \
    chmod 600 /root/.gradle/gradle.properties && \
    gradle clean build -x test

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/*.jar ./app.jar