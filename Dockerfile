FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Build files first so this layer (and the Gradle distribution it downloads) is
# reused until a dependency actually changes.
COPY gradlew settings.gradle build.gradle lombok.config ./
COPY gradle ./gradle
COPY auth/build.gradle auth/
COPY common/build.gradle common/
COPY media/build.gradle media/
COPY notification/build.gradle notification/
COPY product/build.gradle product/
COPY profile/build.gradle profile/
RUN chmod +x gradlew && ./gradlew --no-daemon projects

COPY . .
# The wrapper, not a system gradle: Spring Boot 4.1's plugin needs Gradle 8.14+/9.x.
# bootJar only, so build/libs holds exactly one jar.
RUN ./gradlew --no-daemon clean bootJar -x test


FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# ffmpeg is exec'd by ImageProcessingServiceImpl; curl backs the compose healthcheck
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/build/libs/*.jar ./app.jar

RUN groupadd --system simsim \
    && useradd --system --gid simsim simsim \
    && chown -R simsim:simsim /app
USER simsim

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
