from datetime import timedelta

from temporalio import workflow

# Pass-through import: brings the typed activity reference into the workflow
# without pulling it into the Python SDK's per-task workflow sandbox reload.
with workflow.unsafe.imports_passed_through():
    from .activities import deliver_webhook

from .shared import WebhookDelivery


@workflow.defn
class WebhookWorkflow:
    """Provided for the cost comparison in Module 01. You don't need to edit this."""

    @workflow.run
    async def run(self, req: WebhookDelivery) -> int:
        return await workflow.execute_activity(
            deliver_webhook,
            req,
            start_to_close_timeout=timedelta(seconds=10),
        )
