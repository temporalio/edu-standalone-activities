"""Fire the same webhook delivery wrapped in a Workflow - the comparison shape."""

import asyncio
import sys

from temporalio.client import Client

from .shared import ECHO_SERVER_URL, TASK_QUEUE, WebhookDelivery
from .workflow import WebhookWorkflow


async def main(event_id: str) -> None:
    client = await Client.connect("localhost:7233")
    result = await client.execute_workflow(
        WebhookWorkflow.run,
        args=[WebhookDelivery(
            url=ECHO_SERVER_URL,
            payload={"event_id": event_id, "type": "order.created", "amount": 99.99},
            event_id=event_id,
        )],
        id=f"wf-{event_id}",
        task_queue=TASK_QUEUE,
    )
    print(f"Workflow completed with activity returning status {result}")


if __name__ == "__main__":
    event_id = sys.argv[1] if len(sys.argv) > 1 else "evt_002"
    asyncio.run(main(event_id))
