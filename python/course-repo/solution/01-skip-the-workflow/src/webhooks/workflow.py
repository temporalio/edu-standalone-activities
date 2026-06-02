from datetime import timedelta

from temporalio import workflow

from .shared import WebhookDelivery


@workflow.defn
class WebhookWorkflow:
    """Tiny wrapper around deliver_webhook for the cost comparison in Module 01.

    The activity is referenced by name ("deliver_webhook") rather than by import
    so the workflow sandbox doesn't pull httpx through.
    """

    @workflow.run
    async def run(self, req: WebhookDelivery) -> int:
        return await workflow.execute_activity(
            "deliver_webhook",
            req,
            start_to_close_timeout=timedelta(seconds=10),
        )
