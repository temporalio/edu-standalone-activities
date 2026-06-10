import httpx
from temporalio import activity

from .shared import WebhookDelivery


@activity.defn
def deliver_webhook(req: WebhookDelivery) -> int:
    activity.logger.info(
        "Delivering webhook for event %s to %s", req.event_id, req.url
    )
    # TODO: POST req.payload to req.url using httpx.post()
    # TODO: raise on non-2xx response (response.raise_for_status())
    # TODO: return the HTTP status code
    raise NotImplementedError("Fill in deliver_webhook")
