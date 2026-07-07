package webhook

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"

	"go.temporal.io/sdk/activity"
	"go.temporal.io/sdk/temporal"
)

func DeliverWebhook(ctx context.Context, req WebhookDelivery) (int, error) {
	attempt := activity.GetInfo(ctx).Attempt
	activity.GetLogger(ctx).Info("Delivering webhook", "eventId", req.EventID, "attempt", attempt)

	body, _ := json.Marshal(req.Payload)
	httpReq, err := http.NewRequestWithContext(ctx, http.MethodPost, req.URL, bytes.NewReader(body))
	if err != nil {
		return 0, err
	}
	httpReq.Header.Set("Content-Type", "application/json")
	// The event id is stable across retries, so every retry POSTs the same
	// logical delivery key and the receiver dedupes the side effect.
	httpReq.Header.Set("Idempotency-Key", "webhook:"+req.EventID)

	resp, err := http.DefaultClient.Do(httpReq)
	if err != nil {
		return 0, err
	}
	defer resp.Body.Close()
	if resp.StatusCode >= 300 {
		return 0, fmt.Errorf("HTTP %d", resp.StatusCode)
	}

	// Simulate a transient failure on attempts 1-2 so Temporal retries and the
	// same delivery is POSTed three times. A stable Idempotency-Key keeps the
	// receiver from processing it more than once.
	if attempt < 3 {
		return 0, temporal.NewApplicationError(
			fmt.Sprintf("Simulated transient failure on attempt %d", attempt), "TransientError")
	}
	return resp.StatusCode, nil
}
