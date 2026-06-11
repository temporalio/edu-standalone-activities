"""Submit the same deliver_webhook Activity as a step inside a Workflow.

Pairs with send_standalone.py - same Activity code, second submission path.
This is the "graduate to a Workflow" upgrade with no rewrite of the Activity.
"""

import asyncio
import sys

from temporalio.client import Client

from .shared import WEBHOOK_RECEIVER_URL, TASK_QUEUE, WebhookDelivery
from .workflow import WebhookWorkflow


async def main(event_id: str) -> None:
    client = await Client.connect("localhost:7233")
    # execute_workflow runs the Workflow, which internally calls
    # deliver_webhook - the same Activity send_standalone.py submits directly.
    result = await client.execute_workflow(
        WebhookWorkflow.run,
        args=[WebhookDelivery(
            url=WEBHOOK_RECEIVER_URL,
            payload={"event_id": event_id, "type": "order.created", "amount": 99.99},
            event_id=event_id,
        )],
        id=f"wf-{event_id}",
        task_queue=TASK_QUEUE,
    )
    print(f"Workflow completed with Activity returning status {result}")


if __name__ == "__main__":
    event_id = sys.argv[1] if len(sys.argv) > 1 else "evt_002"
    asyncio.run(main(event_id))
