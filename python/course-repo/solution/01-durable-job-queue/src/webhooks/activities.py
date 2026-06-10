import httpx
from temporalio import activity

from .shared import WebhookDelivery


@activity.defn
def deliver_webhook(req: WebhookDelivery) -> int:
    activity.logger.info(
        "Delivering webhook for event %s to %s", req.event_id, req.url
    )
    response = httpx.post(req.url, json=req.payload, timeout=5.0)
    response.raise_for_status()
    return response.status_code
