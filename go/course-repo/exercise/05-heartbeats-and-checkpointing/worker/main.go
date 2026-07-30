package main

import (
	"log"
	"time"

	"standaloneactivities/exercise/05-heartbeats-and-checkpointing/webhook"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"
)

func main() {
	c, err := client.Dial(client.Options{HostPort: "localhost:7233"})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	w := worker.New(c, webhook.TaskQueue, worker.Options{
		// The SDK batches heartbeats, flushing at most one per throttle interval
		// (80% of HeartbeatTimeout by default = 9.6s here). That is far coarser
		// than this Activity's 1-item-per-second progress, so the server's
		// checkpoint would lag badly behind a crash. Pin the flush to 1s so the
		// stored checkpoint stays within an item of what was actually delivered.
		MaxHeartbeatThrottleInterval: 1 * time.Second,
	})
	w.RegisterActivity(webhook.DeliverWebhookBatch)

	log.Printf("Worker running on task queue %q", webhook.TaskQueue)
	if err := w.Run(worker.InterruptCh()); err != nil {
		log.Fatalln("Unable to start worker", err)
	}
}
