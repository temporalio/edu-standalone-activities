package webhook

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"go.temporal.io/sdk/activity"
)

// DeliverWebhookBatch delivers each item in the batch one at a time,
// checkpointing progress via Heartbeat so a retry can resume instead of
// redelivering everything from the start.
func DeliverWebhookBatch(ctx context.Context, req WebhookDeliveryBatch) (int, error) {
	logger := activity.GetLogger(ctx)

	// On retry, resume from the last checkpoint instead of redoing item 0..n.
	startIndex := 0
	if activity.HasHeartbeatDetails(ctx) {
		var checkpoint int
		if err := activity.GetHeartbeatDetails(ctx, &checkpoint); err == nil {
			startIndex = checkpoint
			logger.Info("Resuming from checkpoint", "startIndex", startIndex, "attempt", activity.GetInfo(ctx).Attempt)
		}
	}

	delivered := startIndex
	for i := startIndex; i < len(req.Items); i++ {
		body, _ := json.Marshal(req.Items[i])
		resp, err := http.Post(req.URL, "application/json", bytes.NewReader(body))
		if err != nil {
			return delivered, err
		}
		func() { defer resp.Body.Close() }()
		if resp.StatusCode >= 300 {
			return delivered, fmt.Errorf("HTTP %d", resp.StatusCode)
		}
		delivered++
		// Checkpoint after each item; a future retry reads this back.
		activity.RecordHeartbeat(ctx, delivered)
		time.Sleep(1 * time.Second)
	}
	return delivered, nil
}
