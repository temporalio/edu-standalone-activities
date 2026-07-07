FROM golang:1.24-bookworm

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates curl wget git jq procps \
    && rm -rf /var/lib/apt/lists/*

# Temporal CLI v1.7.2 ships `temporal activity` subcommands + the Standalone Activities UI tab.
ARG TEMPORAL_VERSION=v1.7.2
RUN ARCH=$(dpkg --print-architecture) && \
    if [ "$ARCH" = "amd64" ]; then ARCH_NAME=amd64; else ARCH_NAME=arm64; fi && \
    VER="${TEMPORAL_VERSION#v}" && \
    curl -fsSL "https://github.com/temporalio/cli/releases/download/${TEMPORAL_VERSION}/temporal_cli_${VER}_linux_${ARCH_NAME}.tar.gz" \
        -o /tmp/temporal.tar.gz && \
    tar -xzf /tmp/temporal.tar.gz -C /usr/local/bin temporal && \
    rm /tmp/temporal.tar.gz && chmod +x /usr/local/bin/temporal

WORKDIR /opt/workshop
COPY course-repo/go.mod course-repo/go.sum ./
RUN go mod download

# Copy the course source; pre-warm/validate the build (compiles every solution + stub).
COPY course-repo/ .
RUN go build ./...

# Build the server binaries onto PATH.
RUN go build -o /usr/local/bin/webhook-receiver ./server/webhookreceiver && \
    go build -o /usr/local/bin/demo-server ./server/demoserver

RUN chmod +x /opt/workshop/scripts/*.sh
EXPOSE 7233 8233 9000 9001
