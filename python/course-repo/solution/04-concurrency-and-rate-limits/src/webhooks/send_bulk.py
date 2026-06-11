"""Fan out N webhook deliveries as Standalone Activities and wait for all."""

import asyncio
import sys
from datetime import timedelta

from temporalio.client import Client

from .activities import deliver_webhook
from .shared import WEBHOOK_RECEIVER_URL, TASK_QUEUE, WebhookDelivery


async def main(count: int = 30) -> None:
    client = await Client.connect("localhost:7233")
    handles = []
    for i in range(count):
        h = await client.start_activity(
            deliver_webhook,
            args=[WebhookDelivery(
                url=WEBHOOK_RECEIVER_URL,
                payload={"event_id": f"bulk_{i:03d}", "type": "bulk_send"},
                event_id=f"bulk_{i:03d}",
            )],
            id=f"bulk-{i:03d}",
            task_queue=TASK_QUEUE,
            start_to_close_timeout=timedelta(seconds=30),
        )
        handles.append(h)
    await asyncio.gather(*(h.result() for h in handles))
    print(f"All {count} deliveries completed.")


if __name__ == "__main__":
    asyncio.run(main(int(sys.argv[1]) if len(sys.argv) > 1 else 30))
