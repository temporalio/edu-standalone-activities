"""Submit a batch webhook delivery as a single long-running Standalone Activity.

The Activity heartbeats progress between items. Kill the Worker mid-batch
and the next attempt resumes from the last reported checkpoint instead
of redoing items already delivered.
"""

import asyncio
import sys
from datetime import timedelta

from temporalio.client import Client

from .activities import deliver_webhook_batch
from .shared import WEBHOOK_RECEIVER_URL, TASK_QUEUE, WebhookDeliveryBatch


async def main(count: int) -> None:
    client = await Client.connect("localhost:7233")

    items = [
        {"event_id": f"item_{i:03d}", "type": "batch.delivery", "index": i}
        for i in range(count)
    ]

    result = await client.execute_activity(
        deliver_webhook_batch,
        args=[WebhookDeliveryBatch(url=WEBHOOK_RECEIVER_URL, items=items)],
        id=f"deliver-batch-{count}",
        task_queue=TASK_QUEUE,
        start_to_close_timeout=timedelta(minutes=5),
        # heartbeat_timeout: if no heartbeat for 5s, Temporal treats the
        # attempt as dead and retries — picking up from the last checkpoint.
        # Without this, a crashed attempt is only noticed at start_to_close,
        # which could be many minutes.
        heartbeat_timeout=timedelta(seconds=5),
    )
    print(f"Batch delivery completed: {result} items delivered.")


if __name__ == "__main__":
    count = int(sys.argv[1]) if len(sys.argv) > 1 else 10
    asyncio.run(main(count))
