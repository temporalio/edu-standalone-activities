package webhook

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"time"
)

// DeliverWebhookBatch delivers each item in the batch one at a time.
func DeliverWebhookBatch(ctx context.Context, req WebhookDeliveryBatch) (int, error) {
	startIndex := 0
	// TODO 1: if activity.HasHeartbeatDetails(ctx), read the checkpoint with
	//         activity.GetHeartbeatDetails(ctx, &checkpoint) and resume from it.

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
		// TODO 2: activity.RecordHeartbeat(ctx, delivered) to checkpoint each item.
		time.Sleep(1 * time.Second)
	}
	return delivered, nil
}
