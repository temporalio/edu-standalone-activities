FROM eclipse-temurin:21-jdk

# System deps
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates curl wget git jq procps \
    && rm -rf /var/lib/apt/lists/*

# Maven (download the binary directly so we don't pull a second JDK via apt).
# Use archive.apache.org, which keeps every release permanently; the dlcdn mirror
# only serves the current release and 404s once a version is superseded.
ARG MAVEN_VERSION=3.9.9
RUN curl -fsSL "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
        -o /tmp/maven.tar.gz \
    && tar -xzf /tmp/maven.tar.gz -C /opt \
    && rm /tmp/maven.tar.gz \
    && ln -s "/opt/apache-maven-${MAVEN_VERSION}/bin/mvn" /usr/local/bin/mvn

# Temporal CLI - v1.7.2 stable ships `temporal activity` subcommands + the Standalone Activities UI tab.
ARG TEMPORAL_VERSION=v1.7.2
RUN ARCH=$(dpkg --print-architecture) && \
    if [ "$ARCH" = "amd64" ]; then ARCH_NAME=amd64; else ARCH_NAME=arm64; fi && \
    VER="${TEMPORAL_VERSION#v}" && \
    curl -fsSL "https://github.com/temporalio/cli/releases/download/${TEMPORAL_VERSION}/temporal_cli_${VER}_linux_${ARCH_NAME}.tar.gz" \
        -o /tmp/temporal.tar.gz && \
    tar -xzf /tmp/temporal.tar.gz -C /usr/local/bin temporal && \
    rm /tmp/temporal.tar.gz && \
    chmod +x /usr/local/bin/temporal

WORKDIR /opt/workshop
COPY course-repo/ .

# 1. Install the parent POM into the local repo so each seeded module directory
#    can resolve it by coordinates at runtime.
# 2. Build the support-server jar (webhook receiver + demo server).
# 3. Compile every module (reactor) to validate all stubs and solutions and to
#    pre-download the Temporal SDK + compiler plugin into /root/.m2.
# 4. Pre-fetch the exec plugin so `mvn exec:java` starts fast (and offline) at runtime.
RUN mvn -q -N install \
    && mvn -q -f server/pom.xml package \
    && mvn -q compile \
    && mvn -q org.codehaus.mojo:exec-maven-plugin:3.5.0:help -N > /dev/null

RUN chmod +x /opt/workshop/scripts/*.sh

EXPOSE 7233 8233 9000 9001
