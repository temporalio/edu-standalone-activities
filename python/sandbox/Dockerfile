FROM python:3.13-slim

ENV DEBIAN_FRONTEND=noninteractive \
    PYTHONUNBUFFERED=1 \
    PATH="/root/.local/bin:/usr/local/bin:${PATH}"

RUN apt-get update && apt-get install -y --no-install-recommends \
    curl ca-certificates git tmux procps jq rsync \
    && rm -rf /var/lib/apt/lists/*

# uv (Python package manager)
RUN curl -LsSf https://astral.sh/uv/install.sh | sh

# Prerelease Temporal CLI with standalone-activity support.
# TODO: pin to the exact release tag once one is published. This is a placeholder.
ARG TEMPORAL_CLI_TAG=v1.6.2-standalone-activity
RUN curl -L -o /tmp/temporal.tgz \
      "https://github.com/temporalio/cli/releases/download/${TEMPORAL_CLI_TAG}/temporal_cli_${TEMPORAL_CLI_TAG#v}_linux_amd64.tar.gz" \
    && tar -xzf /tmp/temporal.tgz -C /usr/local/bin temporal \
    && rm /tmp/temporal.tgz \
    && chmod +x /usr/local/bin/temporal

# Pre-clone course repo
COPY course-repo/ /opt/workshop/
RUN cd /opt/workshop && /root/.local/bin/uv sync

EXPOSE 7233 8233 9000

CMD ["sleep", "infinity"]
