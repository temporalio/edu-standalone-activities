"""Fire a single webhook delivery as a Standalone Activity."""

import asyncio
import sys
from datetime import timedelta

from temporalio.client import Client

from .activities import deliver_webhook
from .shared import WEBHOOK_RECEIVER_URL, TASK_QUEUE, WebhookDelivery


async def main(event_id: str) -> None:
    client = await Client.connect("localhost:7233")
    result = await client.execute_activity(
        deliver_webhook,
        args=[WebhookDelivery(
            url=WEBHOOK_RECEIVER_URL,
            payload={"event_id": event_id, "type": "order.created"},
            event_id=event_id,
        )],
        id=f"deliver-{event_id}",
        task_queue=TASK_QUEUE,
        start_to_close_timeout=timedelta(seconds=30),
    )
    print(f"Standalone activity completed with status {result}")


if __name__ == "__main__":
    event_id = sys.argv[1] if len(sys.argv) > 1 else "evt_001"
    asyncio.run(main(event_id))
