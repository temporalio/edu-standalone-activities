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

    # activity_id is stable across retries; perfect as an idempotency key.
    # Every retry POSTs with the same key, so the receiver can recognise
    # duplicates and return the cached response instead of recording a
    # new delivery.
    headers = {"Idempotency-Key": info.activity_id}

    response = httpx.post(req.url, json=req.payload, headers=headers, timeout=10.0)
    response.raise_for_status()

    # Simulate a transient downstream failure on the first two attempts -
    # see the exercise version of this file for the full reasoning.
    if info.attempt < 3:
        raise ApplicationError(
            f"Simulated transient failure on attempt {info.attempt}",
            non_retryable=False,
        )

    return response.status_code
