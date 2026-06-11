from datetime import timedelta

from temporalio import workflow

# Pass-through imports: bring activity + shared types into the workflow
# without pulling them into the Python SDK's per-task workflow sandbox reload.
with workflow.unsafe.imports_passed_through():
    from .activities import deliver_webhook
    from .shared import WebhookDelivery


@workflow.defn
class WebhookWorkflow:
    """Tiny Workflow wrapper around deliver_webhook. The standalone caller in
    send_standalone.py and this Workflow both use the same Activity function —
    the "same code, two job types" demo lands fully in Module 06.
    """

    @workflow.run
    async def run(self, req: WebhookDelivery) -> int:
        return await workflow.execute_activity(
            deliver_webhook,
            req,
            start_to_close_timeout=timedelta(seconds=10),
        )
