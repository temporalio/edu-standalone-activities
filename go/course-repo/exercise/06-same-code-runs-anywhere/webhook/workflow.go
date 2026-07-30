package webhook

import (
	"time"

	"go.temporal.io/sdk/workflow"
)

// WebhookWorkflow runs the SAME DeliverWebhook Activity as a Workflow step.
func WebhookWorkflow(ctx workflow.Context, req WebhookDelivery) (int, error) {
	ctx = workflow.WithActivityOptions(ctx, workflow.ActivityOptions{
		StartToCloseTimeout: 10 * time.Second,
	})
	var status int
	err := workflow.ExecuteActivity(ctx, DeliverWebhook, req).Get(ctx, &status)
	return status, err
}
