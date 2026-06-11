"""Rate-limit pain demo: fan out N deliveries using `demo-*` Activity IDs.

Used by Module 04 Section 2 (the "real rate limit on the receiver" demo). Same
fan-out shape as send_bulk, but a separate `demo-*` ID prefix so the leftover
in-flight Activities from this demo do not collide with the `bulk-*` Activities
in Sections 1, 3, and 4.
"""

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
                payload={"event_id": f"demo_{i:03d}", "type": "demo_rate_limit"},
                event_id=f"demo_{i:03d}",
            )],
            id=f"demo-{i:03d}",
            task_queue=TASK_QUEUE,
            start_to_close_timeout=timedelta(seconds=30),
        )
        handles.append(h)
    await asyncio.gather(*(h.result() for h in handles))
    print(f"All {count} deliveries completed.")


if __name__ == "__main__":
    asyncio.run(main(int(sys.argv[1]) if len(sys.argv) > 1 else 30))
