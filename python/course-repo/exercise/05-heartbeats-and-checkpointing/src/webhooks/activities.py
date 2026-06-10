import time

import httpx
from temporalio import activity

from .shared import WebhookDeliveryBatch


# Process a list of webhooks as a single long-running Standalone Activity.
# Heartbeat progress after each delivery so a retry can resume from the
# last checkpoint instead of redoing items already delivered.
@activity.defn
def deliver_webhook_batch(req: WebhookDeliveryBatch) -> int:
    info = activity.info()

    # TODO: on retry, read activity.info().heartbeat_details to find the
    # last reported progress and resume from there. Without this read,
    # every retry starts at item 0 even though the previous attempt
    # heartbeated its progress.
    start_index = 0

    delivered = start_index
    for i in range(start_index, len(req.items)):
        item = req.items[i]
        response = httpx.post(req.url, json=item, timeout=5.0)
        response.raise_for_status()
        delivered += 1
        # Report progress to Temporal — the server uses this for liveness
        # detection (heartbeat_timeout) and stores it so the NEXT attempt
        # can read it back via activity.info().heartbeat_details.
        activity.heartbeat(delivered)
        # Slow enough that you can run kill-worker.sh mid-batch in the demo.
        time.sleep(1)

    return delivered
