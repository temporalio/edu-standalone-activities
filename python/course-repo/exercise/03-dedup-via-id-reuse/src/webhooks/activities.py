import time

import httpx
from temporalio import activity

from .shared import WebhookDelivery


@activity.defn
def deliver_webhook(req: WebhookDelivery) -> int:
    activity.logger.info(
        "Delivering webhook for event %s to %s", req.event_id, req.url
    )
    headers = {"Idempotency-Key": activity.info().activity_id}
    response = httpx.post(req.url, json=req.payload, headers=headers, timeout=10.0)
    response.raise_for_status()
    # Tiny pause so the second duplicate-call lands while this activity is
    # still in-flight (id_conflict_policy applies; id_reuse_policy does not).
    time.sleep(1)
    return response.status_code
