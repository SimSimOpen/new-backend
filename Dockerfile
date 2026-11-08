# The official Gradle image ships Gradle itself, so the build never downloads a
# distribution: the wrapper's URL redirects to GitHub Releases, and a GitHub outage
# then broke every image build. Keep this tag in step with gradle-wrapper.properties
# (Spring Boot 4.1's plugin needs Gradle 8.14+/9.x).
FROM gradle:9.5.1-jdk21-jammy AS build
WORKDIR /app

# Build files first so this layer (plugin resolution) is reused until a build
# file actually changes.
COPY settings.gradle build.gradle lombok.config ./
COPY auth/build.gradle auth/
COPY common/build.gradle common/
COPY media/build.gradle media/
COPY notification/build.gradle notification/
COPY notify/build.gradle notify/
COPY product/build.gradle product/
COPY profile/build.gradle profile/
RUN gradle --no-daemon projects

COPY . .
# bootJar only, so build/libs holds exactly one jar.
RUN gradle --no-daemon clean bootJar -x test


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
