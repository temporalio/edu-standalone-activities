"""Submit a webhook delivery as a Standalone Activity - Temporal's durable job queue."""

import asyncio
import sys
from datetime import timedelta

from temporalio.client import Client

from .activities import deliver_webhook
from .shared import WEBHOOK_RECEIVER_URL, TASK_QUEUE, WebhookDelivery


async def main(event_id: str) -> None:
    client = await Client.connect("localhost:7233")
    # execute_activity submits the Activity as a top-level job and waits for the result.
    # Durably persisted, retried on failure, addressable in the UI - one API call.
    result = await client.execute_activity(
        deliver_webhook,
        args=[WebhookDelivery(
            url=WEBHOOK_RECEIVER_URL,
            payload={"event_id": event_id, "type": "order.created", "amount": 99.99},
            event_id=event_id,
        )],
        id=f"deliver-{event_id}",                      # Addressable job ID - query, cancel, terminate by this handle.
        task_queue=TASK_QUEUE,                         # Workers polling this queue pick up the job.
        start_to_close_timeout=timedelta(seconds=10),  # Declarative timeout - no retry library to wire up.
    )
    print(f"Standalone Activity completed with status {result}")


if __name__ == "__main__":
    event_id = sys.argv[1] if len(sys.argv) > 1 else "evt_001"
    asyncio.run(main(event_id))
