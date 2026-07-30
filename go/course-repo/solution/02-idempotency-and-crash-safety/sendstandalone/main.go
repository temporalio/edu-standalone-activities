package main

import (
	"context"
	"log"
	"os"
	"time"

	"standaloneactivities/solution/02-idempotency-and-crash-safety/webhook"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/temporal"
)

func main() {
	eventID := "evt_001"
	if len(os.Args) > 1 {
		eventID = os.Args[1]
	}

	c, err := client.Dial(client.Options{HostPort: "localhost:7233"})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	req := webhook.WebhookDelivery{
		URL:     webhook.WebhookReceiverURL,
		Payload: map[string]any{"eventId": eventID, "type": "order.created", "amount": 99.99},
		EventID: eventID,
	}

	// One API call submits the durable job. The job survives even if this
	// process exits before the Activity completes: the Worker keeps retrying
	// and the result stays available server-side until it's fetched.
	handle, err := c.ExecuteActivity(context.Background(), client.StartActivityOptions{
		ID:                  "deliver-" + eventID,
		TaskQueue:           webhook.TaskQueue,
		StartToCloseTimeout: 10 * time.Second,
		RetryPolicy:         &temporal.RetryPolicy{MaximumAttempts: 5},
	}, webhook.DeliverWebhook, req)
	if err != nil {
		log.Fatalln("Unable to start standalone activity", err)
	}

	var status int
	if err := handle.Get(context.Background(), &status); err != nil {
		log.Fatalln("Standalone activity failed", err)
	}
	log.Printf("Activity completed with status %d", status)
}
