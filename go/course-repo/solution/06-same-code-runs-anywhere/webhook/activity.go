package webhook

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"

	"go.temporal.io/sdk/activity"
)

// DeliverWebhook is a regular Go function. Standalone vs. inside-a-Workflow is
// decided by HOW it's called, not how it's defined.
func DeliverWebhook(ctx context.Context, req WebhookDelivery) (int, error) {
	activity.GetLogger(ctx).Info("Delivering webhook", "eventId", req.EventID, "url", req.URL)

	body, _ := json.Marshal(req.Payload)
	resp, err := http.Post(req.URL, "application/json", bytes.NewReader(body))
	if err != nil {
		return 0, err // network error: Temporal retries
	}
	defer resp.Body.Close()

	if resp.StatusCode >= 300 {
		return 0, fmt.Errorf("HTTP %d", resp.StatusCode) // 4xx/5xx: Temporal retries
	}
	return resp.StatusCode, nil
}
