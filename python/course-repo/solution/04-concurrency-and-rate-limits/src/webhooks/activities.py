import httpx
from temporalio import activity

from .shared import WebhookDelivery


@activity.defn
def deliver_webhook(req: WebhookDelivery) -> int:
    activity.logger.info(
        "Delivering webhook for event %s to %s", req.event_id, req.url
    )
    # Idempotency-Key from Module 02 - keeps retries from duplicating this event.
    headers = {"Idempotency-Key": f"webhook:{req.event_id}"}
    response = httpx.post(req.url, json=req.payload, headers=headers, timeout=10.0)
    response.raise_for_status()
    return response.status_code
