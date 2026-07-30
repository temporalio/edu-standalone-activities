package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"strconv"
	"time"

	"standaloneactivities/exercise/05-heartbeats-and-checkpointing/webhook"

	"go.temporal.io/sdk/client"
)

func main() {
	count := 10
	if len(os.Args) > 1 {
		if n, err := strconv.Atoi(os.Args[1]); err == nil {
			count = n
		}
	}
	c, err := client.Dial(client.Options{HostPort: "localhost:7233"})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	items := make([]map[string]any, count)
	for i := range items {
		items[i] = map[string]any{"eventId": fmt.Sprintf("item_%03d", i), "type": "batch.delivery", "index": i}
	}

	// Optional second arg labels the Activity ID (e.g. `sendbatch 10 fixed` ->
	// deliver-batch-10-fixed). Section 3 uses a label so its execution is its own
	// Activity, separate from the one section 1 already ran to completion.
	activityID := fmt.Sprintf("deliver-batch-%d", count)
	if len(os.Args) > 2 && os.Args[2] != "" {
		activityID = fmt.Sprintf("%s-%s", activityID, os.Args[2])
	}

	handle, err := c.ExecuteActivity(context.Background(), client.StartActivityOptions{
		ID:                  activityID,
		TaskQueue:           webhook.TaskQueue,
		StartToCloseTimeout: 5 * time.Minute,
		// Sized above the full 10-item batch runtime (10 items x 1s). Section 1's
		// Activity never heartbeats, so its timer runs from attempt start: any
		// value below the batch runtime would kill every attempt mid-batch and
		// retry forever instead of retrying once and finishing.
		HeartbeatTimeout: 12 * time.Second,
	}, webhook.DeliverWebhookBatch, webhook.WebhookDeliveryBatch{URL: webhook.WebhookReceiverURL, Items: items})
	if err != nil {
		// An already-started error means a previous execution of this same
		// Activity ID is still in flight. Nothing is delivered in that case, so
		// clear it out rather than reading the stale counts as a result.
		log.Fatalf("submit failed for %s: %v\n"+
			"If this says the Activity is already started, run scripts/reset-batch.sh first.",
			activityID, err)
	}

	var delivered int
	if err := handle.Get(context.Background(), &delivered); err != nil {
		log.Fatalln("batch delivery failed", err)
	}
	log.Printf("Batch delivery completed: %d items delivered.", delivered)
}
