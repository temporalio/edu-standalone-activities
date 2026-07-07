package webhook

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"

	"go.temporal.io/sdk/activity"
)

func DeliverWebhook(ctx context.Context, req WebhookDelivery) (int, error) {
	info := activity.GetInfo(ctx)
	activity.GetLogger(ctx).Info("Delivering webhook", "eventId", req.EventID, "attempt", info.Attempt)

	body, _ := json.Marshal(req.Payload)
	httpReq, err := http.NewRequestWithContext(ctx, http.MethodPost, req.URL, bytes.NewReader(body))
	if err != nil {
		return 0, err
	}
	httpReq.Header.Set("Content-Type", "application/json")
	// Stable idempotency key: the receiver dedupes retries of the SAME event.
	httpReq.Header.Set("Idempotency-Key", req.EventID)

	resp, err := http.DefaultClient.Do(httpReq)
	if err != nil {
		return 0, err
	}
	defer resp.Body.Close()
	if resp.StatusCode >= 300 {
		return 0, fmt.Errorf("HTTP %d", resp.StatusCode)
	}
	return resp.StatusCode, nil
}
