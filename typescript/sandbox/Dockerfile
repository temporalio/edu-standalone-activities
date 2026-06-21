FROM node:22-bookworm-slim

# System deps
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates curl wget git jq procps \
    && rm -rf /var/lib/apt/lists/*

# Temporal CLI - v1.7.2 stable ships `temporal activity` subcommands.
ARG TEMPORAL_VERSION=v1.7.2
RUN ARCH=$(dpkg --print-architecture) && \
    if [ "$ARCH" = "amd64" ]; then ARCH_NAME=amd64; else ARCH_NAME=arm64; fi && \
    VER="${TEMPORAL_VERSION#v}" && \
    curl -fsSL "https://github.com/temporalio/cli/releases/download/${TEMPORAL_VERSION}/temporal_cli_${VER}_linux_${ARCH_NAME}.tar.gz" \
        -o /tmp/temporal.tar.gz && \
    tar -xzf /tmp/temporal.tar.gz -C /usr/local/bin temporal && \
    rm /tmp/temporal.tar.gz && \
    chmod +x /usr/local/bin/temporal

# Install ts-node and typescript globally so attendees can run `ts-node src/worker.ts`
# from any module directory without an npm install step.
RUN npm install -g ts-node typescript

# Copy the workshop source and install dependencies.
WORKDIR /opt/workshop
COPY course-repo/package.json course-repo/package-lock.json* ./
RUN npm install

# Copy the rest of the course-repo content.
COPY course-repo/ .

# Pre-warm the TypeScript compiler cache so module 01's Worker starts faster.
# This also validates that the solution files compile cleanly at image build time.
RUN ts-node --version

# Make scripts executable.
RUN chmod +x /opt/workshop/scripts/*.sh

EXPOSE 7233 8233 9000
