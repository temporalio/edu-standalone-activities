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

	handle, err := c.ExecuteActivity(context.Background(), client.StartActivityOptions{
		ID:                  fmt.Sprintf("deliver-batch-%d", count),
		TaskQueue:           webhook.TaskQueue,
		StartToCloseTimeout: 5 * time.Minute,
		HeartbeatTimeout:    5 * time.Second,
	}, webhook.DeliverWebhookBatch, webhook.WebhookDeliveryBatch{URL: webhook.WebhookReceiverURL, Items: items})
	if err != nil {
		log.Fatalln("submit failed", err)
	}

	var delivered int
	if err := handle.Get(context.Background(), &delivered); err != nil {
		log.Fatalln("batch delivery failed", err)
	}
	log.Printf("Batch delivery completed: %d items delivered.", delivered)
}
