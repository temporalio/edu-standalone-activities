package main

import (
	"context"
	"log"
	"os"
	"time"

	"standaloneactivities/exercise/03-dedup-via-id-reuse/webhook"

	"go.temporal.io/sdk/client"
)

func start(ctx context.Context, c client.Client, eventID, label string) client.ActivityHandle {
	log.Printf("[%s] start activityId=deliver-%s", label, eventID)
	handle, err := c.ExecuteActivity(ctx, client.StartActivityOptions{
		ID:                  "deliver-" + eventID,
		TaskQueue:           webhook.TaskQueue,
		StartToCloseTimeout: 30 * time.Second,
		// TODO: add ActivityIDConflictPolicy: enums.ACTIVITY_ID_CONFLICT_POLICY_USE_EXISTING
		// so the second submit returns the existing handle instead of erroring.
	}, webhook.DeliverWebhook, webhook.WebhookDelivery{
		URL:     webhook.WebhookReceiverURL,
		Payload: map[string]any{"eventId": eventID, "type": "dup_test"},
		EventID: eventID,
	})
	if err != nil {
		log.Printf("[%s] FAILED: %v", label, err)
		return nil
	}
	log.Printf("[%s] handle ok (activityId=%s runId=%s)", label, handle.GetID(), handle.GetRunID())
	return handle
}

func main() {
	eventID := "evt_dup"
	if len(os.Args) > 1 {
		eventID = os.Args[1]
	}
	c, err := client.Dial(client.Options{HostPort: "localhost:7233"})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	h1 := start(context.Background(), c, eventID, "call-1")
	h2 := start(context.Background(), c, eventID, "call-2")

	var out int
	if h1 != nil {
		_ = h1.Get(context.Background(), &out)
		log.Println("[call-1] activity completed")
	}
	if h2 != nil {
		_ = h2.Get(context.Background(), &out)
		log.Println("[call-2] activity completed")
	}
}
