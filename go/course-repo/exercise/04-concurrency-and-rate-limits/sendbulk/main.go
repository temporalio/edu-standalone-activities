package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"strconv"
	"sync"
	"time"

	"standaloneactivities/exercise/04-concurrency-and-rate-limits/webhook"

	"go.temporal.io/sdk/client"
)

func main() {
	count := 60
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

	var wg sync.WaitGroup
	for i := 0; i < count; i++ {
		id := fmt.Sprintf("bulk_%03d", i)
		handle, err := c.ExecuteActivity(context.Background(), client.StartActivityOptions{
			ID:                  "bulk-" + id,
			TaskQueue:           webhook.TaskQueue,
			StartToCloseTimeout: 30 * time.Second,
		}, webhook.DeliverWebhook, webhook.WebhookDelivery{
			URL:     webhook.WebhookReceiverURL,
			Payload: map[string]any{"eventId": id, "type": "bulk_send"},
			EventID: id,
		})
		if err != nil {
			log.Fatalln("submit failed", err)
		}
		wg.Add(1)
		go func(h client.ActivityHandle) {
			defer wg.Done()
			var out int
			_ = h.Get(context.Background(), &out)
		}(handle)
	}
	wg.Wait()
	log.Printf("All %d deliveries completed.", count)
}
