package main

import (
	"log"

	"standaloneactivities/exercise/04-concurrency-and-rate-limits/webhook"

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
		// TODO: add WorkerActivitiesPerSecond: 2 (and MaxConcurrentActivityExecutionSize: 10)
		// so the Worker paces dispatch and stops flooding the receiver with 429s.
	})
	w.RegisterActivity(webhook.DeliverWebhook)

	log.Printf("Worker running on task queue %q", webhook.TaskQueue)
	if err := w.Run(worker.InterruptCh()); err != nil {
		log.Fatalln("Unable to start worker", err)
	}
}
