"""Fire start_activity TWICE with the same id, back-to-back.

The second call uses ActivityIDConflictPolicy.USE_EXISTING so it
quietly returns the existing handle instead of erroring.
"""

import asyncio
import sys
from datetime import timedelta

from temporalio.client import Client
from temporalio.common import ActivityIDConflictPolicy

from .activities import deliver_webhook
from .shared import WEBHOOK_RECEIVER_URL, TASK_QUEUE, WebhookDelivery


async def start(client: Client, event_id: str, label: str):
    """Try to start one activity with the given event_id."""
    print(f"[{label}] start_activity id=deliver-{event_id}")
    try:
        handle = await client.start_activity(
            deliver_webhook,
            args=[WebhookDelivery(
                url=WEBHOOK_RECEIVER_URL,
                payload={"event_id": event_id, "type": "dup_test"},
                event_id=event_id,
            )],
            id=f"deliver-{event_id}",
            task_queue=TASK_QUEUE,
            start_to_close_timeout=timedelta(seconds=30),
            # USE_EXISTING: second submission with the same id returns the
            # existing handle instead of erroring. Server-side dedup; the
            # duplicate never reaches a Worker.
            id_conflict_policy=ActivityIDConflictPolicy.USE_EXISTING,
        )
        print(f"[{label}] handle ok (run_id={handle.run_id})")
        return handle
    except Exception as e:
        print(f"[{label}] FAILED: {type(e).__name__}: {e}")
        return None


async def main(event_id: str) -> None:
    client = await Client.connect("localhost:7233")
    h1 = await start(client, event_id, "call-1")
    h2 = await start(client, event_id, "call-2")

    if h1 is not None:
        await h1.result()
        print("[call-1] activity completed")
    if h2 is not None:
        await h2.result()
        print("[call-2] activity completed")


if __name__ == "__main__":
    event_id = sys.argv[1] if len(sys.argv) > 1 else "evt_dup"
    asyncio.run(main(event_id))
