import httpx
from temporalio import activity
from temporalio.exceptions import ApplicationError

from .shared import WebhookDelivery


@activity.defn
def deliver_webhook(req: WebhookDelivery) -> int:
    info = activity.info()
    activity.logger.info(
        "Delivering webhook for event %s (attempt %d)", req.event_id, info.attempt
    )

    # The webhook event id is stable across retries, so every retry POSTs the
    # same logical delivery key and the receiver can dedupe the side effect.
    headers = {"Idempotency-Key": f"webhook:{req.event_id}"}

    response = httpx.post(req.url, json=req.payload, headers=headers, timeout=10.0)
    response.raise_for_status()

    # Simulate transient failure on attempts 1-2. ApplicationError defaults to
    # retryable; set non_retryable=True for permanent failures.
    if info.attempt < 3:
        raise ApplicationError(
            f"Simulated transient failure on attempt {info.attempt}",
        )

    return response.status_code
