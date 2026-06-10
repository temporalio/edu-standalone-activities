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

    headers: dict[str, str] = {}
    # TODO: add an Idempotency-Key header from activity.info().activity_id.
    # The webhook receiver caches by this header and dedupes duplicate deliveries.
    # The key MUST be deterministic across retries - use activity.info()
    # .activity_id (stable), not uuid4() (regenerated each attempt).

    response = httpx.post(req.url, json=req.payload, headers=headers, timeout=10.0)
    response.raise_for_status()

    # Simulate a transient downstream failure that hits AFTER the POST has
    # already landed - the receiver got the request, but our activity errored
    # before Temporal heard "done." Real-world equivalents: 500 from the
    # endpoint, network drop after the receiver processed it, worker crash
    # right after the side effect. Temporal retries; each retry replays the
    # POST; without an idempotency key the receiver records every attempt.
    if info.attempt < 3:
        # ApplicationError defaults to retryable; Temporal will retry under
        # the default RetryPolicy. Set non_retryable=True for permanent failures.
        raise ApplicationError(
            f"Simulated transient failure on attempt {info.attempt}",
        )

    return response.status_code
