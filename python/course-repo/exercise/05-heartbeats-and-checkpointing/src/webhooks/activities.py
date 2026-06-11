import time

import httpx
from temporalio import activity
from temporalio.exceptions import CancelledError

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
    try:
        for i in range(start_index, len(req.items)):
            item = req.items[i]
            response = httpx.post(req.url, json=item, timeout=5.0)
            response.raise_for_status()
            delivered += 1
            # Report progress to Temporal — the server uses this for liveness
            # detection (heartbeat_timeout) and stores it so the NEXT attempt
            # can read it back via activity.info().heartbeat_details.
            # heartbeat() is also where cancellation is delivered: if the
            # Activity has been cancelled, the next call raises CancelledError.
            activity.heartbeat(delivered)
            # Slow enough that you can run kill-worker.sh mid-batch in the demo.
            time.sleep(1)
    except CancelledError:
        # Cancellation handler — runs when `temporal activity cancel ...` or
        # an enclosing Workflow cancels this Activity. delivered is already
        # heartbeated, so a retry (if one is scheduled) resumes from here.
        activity.logger.info(
            "Cancelled at index %d (delivered=%d of %d)",
            i, delivered, len(req.items)
        )
        raise

    return delivered
