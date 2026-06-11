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

    # The event id is the logical delivery id for this standalone webhook.
    # Every retry POSTs with the same key, so the receiver can recognize
    # duplicates and return the cached response instead of recording a
    # new delivery.
    headers = {"Idempotency-Key": f"webhook:{req.event_id}"}

    response = httpx.post(req.url, json=req.payload, headers=headers, timeout=10.0)
    response.raise_for_status()

    # Simulate a transient downstream failure on the first two attempts -
    # see the exercise version of this file for the full reasoning.
    if info.attempt < 3:
        # ApplicationError defaults to retryable; Temporal will retry under
        # the default RetryPolicy. Set non_retryable=True for permanent failures.
        raise ApplicationError(
            f"Simulated transient failure on attempt {info.attempt}",
        )

    return response.status_code
