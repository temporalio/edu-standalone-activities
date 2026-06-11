from datetime import timedelta

from temporalio import workflow

# This Workflow uses the SAME deliver_webhook function from activities.py
# that the standalone caller uses. No copies, no rewrites - write the
# Activity once, compose it into a multi-step Workflow when the job grows.
# Both imports go through imports_passed_through so the Python SDK
# sandbox doesn't reload activities.py / shared.py on every workflow task.
with workflow.unsafe.imports_passed_through():
    from .activities import deliver_webhook
    from .shared import WebhookDelivery


@workflow.defn
class WebhookWorkflow:
    """Thin Workflow that calls deliver_webhook as a step.

    Demonstrates that the same Activity code runs as a top-level job
    (client.execute_activity) or as a Workflow step (workflow.execute_activity).
    """

    @workflow.run
    async def run(self, req: WebhookDelivery) -> int:
        return await workflow.execute_activity(
            deliver_webhook,
            req,
            start_to_close_timeout=timedelta(seconds=10),
        )
