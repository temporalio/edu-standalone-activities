package main

import (
	"log"

	"standaloneactivities/solution/04-concurrency-and-rate-limits/webhook"

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
		MaxConcurrentActivityExecutionSize: 10,
		// Cap dispatch rate so we don't 429 the downstream receiver.
		// Excess work waits in the Task Queue on the server.
		WorkerActivitiesPerSecond: 2,
	})
	w.RegisterActivity(webhook.DeliverWebhook)

	log.Printf("Worker running on task queue %q (rate cap: 2/sec)", webhook.TaskQueue)
	if err := w.Run(worker.InterruptCh()); err != nil {
		log.Fatalln("Unable to start worker", err)
	}
}
