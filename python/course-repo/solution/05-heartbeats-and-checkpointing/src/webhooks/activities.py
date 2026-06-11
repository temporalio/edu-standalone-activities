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

    # On retry, heartbeat_details holds the last value passed to
    # activity.heartbeat() in the previous attempt. We checkpoint the
    # count of delivered items, so the retry resumes from there.
    start_index = 0
    if info.heartbeat_details:
        start_index = info.heartbeat_details[0]
        activity.logger.info(
            "Resuming from index %d (attempt %d)", start_index, info.attempt
        )

    delivered = start_index
    try:
        for i in range(start_index, len(req.items)):
            item = req.items[i]
            response = httpx.post(req.url, json=item, timeout=5.0)
            response.raise_for_status()
            delivered += 1
            # Checkpoint after each item - Temporal stores this so a future
            # retry reads it back from activity.info().heartbeat_details.
            # heartbeat() is also where cancellation is delivered: if the
            # Activity has been cancelled, the next call raises CancelledError.
            activity.heartbeat(delivered)
            # Slow enough that you can run kill-worker.sh mid-batch in the demo.
            time.sleep(1)
    except CancelledError:
        # Cancellation handler - runs when `temporal activity cancel ...` or
        # an enclosing Workflow cancels this Activity. delivered is already
        # heartbeated, so a retry (if one is scheduled) resumes from here.
        activity.logger.info(
            "Cancelled at index %d (delivered=%d of %d)",
            i, delivered, len(req.items)
        )
        raise

    return delivered
