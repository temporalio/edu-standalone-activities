from datetime import timedelta

from temporalio import workflow

from .shared import WebhookDelivery


@workflow.defn
class WebhookWorkflow:
    """Provided for the cost comparison in Module 01. You don't need to edit this."""

    @workflow.run
    async def run(self, req: WebhookDelivery) -> int:
        return await workflow.execute_activity(
            "deliver_webhook",
            req,
            start_to_close_timeout=timedelta(seconds=10),
        )
