FROM eclipse-temurin:21-jdk

# System tools + python3. Python runs the shared webhook receiver (:9000) and a static
# demo server (:9001). No Node runtime needed for the Java edition.
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates curl wget git jq procps unzip python3 \
    && rm -rf /var/lib/apt/lists/*

# Temporal CLI v1.7.2 ships the `temporal activity` subcommands and the Standalone Activities
# UI tab, and bundles a dev server that enables Standalone Activities by default.
ARG TEMPORAL_VERSION=v1.7.2
RUN ARCH=$(dpkg --print-architecture) && \
    if [ "$ARCH" = "amd64" ]; then ARCH_NAME=amd64; else ARCH_NAME=arm64; fi && \
    VER="${TEMPORAL_VERSION#v}" && \
    curl -fsSL "https://github.com/temporalio/cli/releases/download/${TEMPORAL_VERSION}/temporal_cli_${VER}_linux_${ARCH_NAME}.tar.gz" \
        -o /tmp/temporal.tar.gz && \
    tar -xzf /tmp/temporal.tar.gz -C /usr/local/bin temporal && \
    rm /tmp/temporal.tar.gz && chmod +x /usr/local/bin/temporal

# Gradle: the build tool the course modules use to compile and run.
ARG GRADLE_VERSION=8.10.2
RUN curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip -d /opt && rm /tmp/gradle.zip && \
    ln -s "/opt/gradle-${GRADLE_VERSION}/bin/gradle" /usr/local/bin/gradle

WORKDIR /opt/workshop
# Build context is the repo root so we can COPY both the Java course modules and the
# shared, SDK-agnostic webhook receiver.
COPY java/course-repo/ /opt/workshop/
COPY shared/webhook-receiver/webhook_receiver.py /opt/workshop/server/webhook_receiver.py

# Pre-warm the Gradle dependency cache and compile every module so the first challenge boots
# fast. This also validates that every solution and exercise stub compiles against the SDK.
RUN set -e; for d in exercise/* solution/*; do \
      echo "pre-building $d"; \
      (cd "$d" && gradle --no-daemon -q build -x test); \
    done

RUN chmod +x /opt/workshop/scripts/*.sh
EXPOSE 7233 8233 9000 9001
