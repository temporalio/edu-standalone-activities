package webhook

import (
	"context"
	"fmt"

	"go.temporal.io/sdk/activity"
)

// DeliverWebhook is a regular Go function. Standalone vs. inside-a-Workflow is
// decided by HOW it's called, not how it's defined.
func DeliverWebhook(ctx context.Context, req WebhookDelivery) (int, error) {
	activity.GetLogger(ctx).Info("Delivering webhook", "eventId", req.EventID, "url", req.URL)
	// TODO 1: POST req.Payload as JSON to req.URL with http.Post.
	// TODO 2: return an error if the response status is >= 300 (Temporal retries).
	// TODO 3: return the response status code on success.
	return 0, fmt.Errorf("TODO: implement DeliverWebhook")
}
