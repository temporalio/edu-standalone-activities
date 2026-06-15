"""Fan-out 20 background + 5 urgent deliveries, with Priority on each call.

Demonstrates the Priority kwarg on start_activity. With a rate-capped worker
and a contended queue, lower-numbered priority_keys are dispatched ahead of
higher-numbered ones.

Run with the rate-capped worker from this module (max_activities_per_second=2).
"""

import asyncio
from datetime import timedelta

from temporalio.client import Client
from temporalio.common import Priority

from .activities import deliver_webhook
from .shared import WEBHOOK_RECEIVER_URL, TASK_QUEUE, WebhookDelivery


async def main() -> None:
    client = await Client.connect("localhost:7233")

    # Submit 20 background deliveries first - they'll fill the queue.
    bg_handles = []
    print("Submitting 20 background deliveries (priority_key=5)...")
    for i in range(20):
        h = await client.start_activity(
            deliver_webhook,
            args=[WebhookDelivery(
                url=WEBHOOK_RECEIVER_URL,
                payload={"event_id": f"bg_{i:03d}", "tenant": "background"},
                event_id=f"bg_{i:03d}",
            )],
            id=f"bg-{i:03d}",
            task_queue=TASK_QUEUE,
            start_to_close_timeout=timedelta(seconds=30),
            priority=Priority(priority_key=5),
        )
        bg_handles.append(h)

    # Small pause so the background work is solidly queued.
    await asyncio.sleep(0.3)

    # Submit 5 urgent deliveries - lower priority_key = higher priority.
    urgent_handles = []
    print("Submitting 5 urgent deliveries (priority_key=1)...")
    for i in range(5):
        h = await client.start_activity(
            deliver_webhook,
            args=[WebhookDelivery(
                url=WEBHOOK_RECEIVER_URL,
                payload={"event_id": f"urgent_{i:03d}", "tenant": "urgent"},
                event_id=f"urgent_{i:03d}",
            )],
            id=f"urgent-{i:03d}",
            task_queue=TASK_QUEUE,
            start_to_close_timeout=timedelta(seconds=30),
            priority=Priority(priority_key=1),
        )
        urgent_handles.append(h)

    await asyncio.gather(*(h.result() for h in bg_handles))
    await asyncio.gather(*(h.result() for h in urgent_handles))
    print("All 25 deliveries completed.")


if __name__ == "__main__":
    asyncio.run(main())
