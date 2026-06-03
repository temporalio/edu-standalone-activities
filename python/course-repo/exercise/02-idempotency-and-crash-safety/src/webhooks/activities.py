import time

import httpx
from temporalio import activity

from .shared import WebhookDelivery


@activity.defn
def deliver_webhook(req: WebhookDelivery) -> int:
    activity.logger.info(
        "Delivering webhook for event %s to %s", req.event_id, req.url
    )

    headers: dict[str, str] = {}
    # TODO: add an Idempotency-Key header from activity.info().activity_id.
    # The echo server caches by this header and dedupes duplicate deliveries.
    # The key MUST be deterministic across retries - use activity.info()
    # .activity_id (stable), not uuid4() (regenerated each attempt).

    response = httpx.post(req.url, json=req.payload, headers=headers, timeout=10.0)
    response.raise_for_status()

    # Sleep AFTER the POST to simulate the (real) window between the external
    # side effect succeeding and Temporal learning about it. If the worker
    # crashes during this window, Temporal will retry - and without an
    # idempotency key, the receiver gets the POST twice.
    time.sleep(4)

    return response.status_code
