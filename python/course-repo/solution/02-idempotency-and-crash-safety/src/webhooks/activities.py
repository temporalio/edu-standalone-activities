import time

import httpx
from temporalio import activity

from .shared import WebhookDelivery


@activity.defn
def deliver_webhook(req: WebhookDelivery) -> int:
    activity.logger.info(
        "Delivering webhook for event %s to %s", req.event_id, req.url
    )

    # activity_id is stable across retries; perfect as an idempotency key.
    # If the worker crashes after the POST but before Temporal acks the
    # success, Temporal retries this activity. The receiver caches by this
    # header and returns the cached response on the duplicate POST, so the
    # delivery happens exactly once even though it was attempted twice.
    headers = {"Idempotency-Key": activity.info().activity_id}

    response = httpx.post(req.url, json=req.payload, headers=headers, timeout=10.0)
    response.raise_for_status()

    # Sleep AFTER the POST to simulate the (real) window between the external
    # side effect succeeding and Temporal learning about it.
    time.sleep(15)

    return response.status_code
