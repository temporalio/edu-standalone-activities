import asyncio
import sys
import time
from datetime import timedelta

from temporalio.client import Client
from temporalio.envconfig import ClientConfig

from activities import greet
from shared import TASK_QUEUE


async def main() -> None:
    name = sys.argv[1] if len(sys.argv) > 1 else "Temporal"

    # Same environment-configuration lookup the Worker uses: TEMPORAL_PROFILE
    # selects the profile, the TOML file supplies address/namespace/credentials,
    # and TEMPORAL_* env vars can override individual fields.
    connect_config = ClientConfig.load_client_connect_config()
    connect_config.setdefault("target_host", "localhost:7233")

    client = await Client.connect(**connect_config)

    address = connect_config.get("target_host")
    namespace = connect_config.get("namespace") or "default"
    print(
        f"Executing Standalone Activity against {address} "
        f"(namespace: {namespace})..."
    )

    # client.execute_activity starts a Standalone Activity: no Workflow is
    # involved. It durably enqueues the Activity, waits for a Worker to run it,
    # and returns its result.
    result = await client.execute_activity(
        greet,
        name,
        id=f"greet-{time.time_ns()}",
        task_queue=TASK_QUEUE,
        start_to_close_timeout=timedelta(seconds=10),
    )

    print(f"Standalone Activity result: {result}")


if __name__ == "__main__":
    asyncio.run(main())
