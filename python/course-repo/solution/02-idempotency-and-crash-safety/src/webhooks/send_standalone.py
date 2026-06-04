"""Schedule a webhook delivery as a Standalone Activity.

Uses start_activity (not execute_activity): the call returns as soon as the
server has accepted the activity, regardless of whether it has finished
running. The starter exits; the worker continues independently.
"""

import asyncio
import sys
from datetime import timedelta

from temporalio.client import Client

from .activities import deliver_webhook
from .shared import ECHO_SERVER_URL, TASK_QUEUE, WebhookDelivery


async def main(event_id: str) -> None:
    client = await Client.connect("localhost:7233")
    handle = await client.start_activity(
        deliver_webhook,
        args=[WebhookDelivery(
            url=ECHO_SERVER_URL,
            payload={"event_id": event_id, "type": "order.created", "amount": 99.99},
            event_id=event_id,
        )],
        id=f"deliver-{event_id}",
        task_queue=TASK_QUEUE,
        start_to_close_timeout=timedelta(seconds=20),
    )
    print(f"Scheduled activity {handle.id}")


if __name__ == "__main__":
    event_id = sys.argv[1] if len(sys.argv) > 1 else "evt_001"
    asyncio.run(main(event_id))
